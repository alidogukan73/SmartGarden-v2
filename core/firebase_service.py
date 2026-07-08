"""
Firebase service.

Handles Firebase Realtime Database communication.
"""

from __future__ import annotations

import threading
import time
from datetime import datetime
from pathlib import Path

import firebase_admin
from core.config import AppConfig, FirebaseConfig
from core.logger import AppLogger
from firebase_admin import credentials, db
from models.command_state import CommandState
from models.sensor_reading import SensorReading


class FirebaseService:
    """
    Firebase Realtime Database service.
    """

    def __init__(self) -> None:
        self._logger = AppLogger().logger
        self._initialized = False

        self._command_state = CommandState(
            auto_mode=True,
            relay=False,
            enabled=True,
            moisture_limit=40,
            pump_duration=10,
        )

        self._sync_thread: threading.Thread | None = None
        self._running = False

        self._command_lock = threading.Lock()

        self._retry_delay = 0.5
        self._max_retry_delay = 30.0

    def initialize(self) -> None:
        """
        Initialize Firebase.
        """

        if self._initialized:
            return

        try:
            credential = credentials.Certificate(
                Path(FirebaseConfig.CREDENTIALS_FILE),
            )

            firebase_admin.initialize_app(
                credential,
                {
                    "databaseURL": FirebaseConfig.DATABASE_URL,
                },
            )

            self._initialized = True

            self.initialize_commands()

            self.start_command_sync()

            self._logger.info(
                "Firebase initialized successfully.",
            )

        except Exception as exc:
            self._logger.exception(exc)
            raise

    def _device_ref(self) -> db.Reference:
        """
        Return device database reference.
        """

        if not self._initialized:
            raise RuntimeError(
                "Firebase is not initialized.",
            )

        return db.reference(
            f"devices/{AppConfig.DEVICE_ID}",
        )

    def initialize_commands(self) -> None:
        """
        Create default command values.
        """

        self._device_ref().child("commands").update(
            {
                "auto_mode": True,
                "relay": False,
                "enabled": True,
                "moisture_limit": 40,
                "pump_duration": 10,
            },
        )

    def update_status(self) -> None:
        """
        Update device status.
        """

        self._device_ref().child("status").update(
            {
                "online": True,
                "version": AppConfig.VERSION,
                "last_seen": datetime.now().isoformat(),
            },
        )

    def update_sensor(
        self,
        reading: SensorReading,
    ) -> None:
        """
        Upload sensor values.
        """

        self._device_ref().child("sensor").update(
            {
                "raw": reading.raw,
                "voltage": round(reading.voltage, 3),
                "moisture": reading.moisture,
                "updated_at": datetime.now().isoformat(),
            },
        )

    def get_commands(self) -> CommandState:
        """
        Return all command values.
        """

        commands = (
            self._device_ref()
            .child(
                "commands",
            )
            .get()
        )

        if commands is None:
            return CommandState(
                auto_mode=True,
                relay=False,
                enabled=True,
                moisture_limit=40,
                pump_duration=10,
            )

        return CommandState(
            auto_mode=bool(
                commands.get(
                    "auto_mode",
                    True,
                ),
            ),
            relay=bool(
                commands.get(
                    "relay",
                    False,
                ),
            ),
            enabled=bool(
                commands.get(
                    "enabled",
                    True,
                ),
            ),
            moisture_limit=int(
                commands.get(
                    "moisture_limit",
                    40,
                ),
            ),
            pump_duration=int(
                commands.get(
                    "pump_duration",
                    10,
                ),
            ),
        )

    def _sync_commands(self) -> None:
        """
        Continuously synchronize commands from Firebase.
        """

        self._logger.info(
            "Command synchronization started.",
        )

        while self._running:

            try:
                new_state = self.get_commands()

                with self._command_lock:
                    self._command_state = new_state

                self._retry_delay = 0.5

            except Exception as exc:

                self._logger.warning(
                    "Firebase unavailable. Retrying in %.1f seconds.",
                    self._retry_delay,
                )

                self._logger.debug(
                    "Firebase error: %s",
                    exc,
                )

                time.sleep(self._retry_delay)

                self._retry_delay = min(
                    self._retry_delay * 2,
                    self._max_retry_delay,
                )

                continue

            time.sleep(FirebaseConfig.COMMAND_SYNC_INTERVAL_SECONDS)

        self._logger.info(
            "Command synchronization stopped.",
        )

    def start_command_sync(self) -> None:
        """
        Start background command synchronization.
        """

        if self._running:
            return

        self._running = True

        self._sync_thread = threading.Thread(
            target=self._sync_commands,
            daemon=True,
            name="FirebaseSync",
        )

        self._sync_thread.start()

    def stop_command_sync(self) -> None:
        """
        Stop background synchronization.
        """

        self._running = False

        if self._sync_thread is not None:

            self._sync_thread.join(timeout=2)

            if self._sync_thread.is_alive():

                self._logger.warning(
                    "Command synchronization thread did not stop gracefully.",
            )

            self._sync_thread = None

    @property
    def command_state(self) -> CommandState:
        """
        Return cached command state.
        """

        with self._command_lock:
            command_state = self._command_state

        return command_state
