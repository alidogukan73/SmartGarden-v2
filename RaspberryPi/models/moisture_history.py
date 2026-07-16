"""
Shared moisture history storage.
"""

from __future__ import annotations

import time
from collections import deque
from dataclasses import dataclass
from typing import Iterable


@dataclass(frozen=True)
class MoistureSample:
    """
    One timestamped moisture reading.
    """

    moisture: int
    timestamp: float


class MoistureHistory:
    """
    Stores recent timestamped moisture readings.

    This class is the single source of truth for:
    - sensor stability checks
    - moisture trend analysis
    - future graph data
    - future learning algorithms
    """

    DEFAULT_MAX_SAMPLES = 20

    def __init__(
        self,
        *,
        max_samples: int = DEFAULT_MAX_SAMPLES,
    ) -> None:

        if max_samples < 2:
            raise ValueError(
                "max_samples must be at least 2.",
            )

        self._samples: deque[MoistureSample] = deque(
            maxlen=max_samples,
        )

    def add(
        self,
        moisture: int,
        *,
        timestamp: float | None = None,
    ) -> MoistureSample:
        """
        Add one moisture reading.
        """

        sample = MoistureSample(
            moisture=int(moisture),
            timestamp=(
                time.monotonic()
                if timestamp is None
                else float(timestamp)
            ),
        )

        self._samples.append(
            sample,
        )

        return sample

    def clear(
        self,
    ) -> None:
        """
        Remove all stored readings.
        """

        self._samples.clear()

    def values(
        self,
    ) -> tuple[int, ...]:
        """
        Return moisture values only.
        """

        return tuple(
            sample.moisture
            for sample in self._samples
        )

    def samples(
        self,
    ) -> tuple[MoistureSample, ...]:
        """
        Return all timestamped samples.
        """

        return tuple(
            self._samples
        )

    def latest(
        self,
    ) -> MoistureSample | None:
        """
        Return the latest sample.
        """

        if not self._samples:
            return None

        return self._samples[-1]

    def first(
        self,
    ) -> MoistureSample | None:
        """
        Return the oldest stored sample.
        """

        if not self._samples:
            return None

        return self._samples[0]

    def last_values(
        self,
        count: int,
    ) -> tuple[int, ...]:
        """
        Return the latest moisture values.
        """

        if count <= 0:
            return ()

        return tuple(
            sample.moisture
            for sample in list(
                self._samples,
            )[-count:]
        )

    def count(
        self,
    ) -> int:
        """
        Return the number of stored readings.
        """

        return len(
            self._samples
        )

    def has_at_least(
        self,
        count: int,
    ) -> bool:
        """
        Return whether enough readings exist.
        """

        return len(
            self._samples
        ) >= count

    def minimum(
        self,
    ) -> int | None:
        """
        Return the minimum moisture value.
        """

        values = self.values()

        if not values:
            return None

        return min(
            values
        )

    def maximum(
        self,
    ) -> int | None:
        """
        Return the maximum moisture value.
        """

        values = self.values()

        if not values:
            return None

        return max(
            values
        )

    def average(
        self,
    ) -> float | None:
        """
        Return the average moisture value.
        """

        values = self.values()

        if not values:
            return None

        return (
            sum(values)
            / len(values)
        )

    def extend(
        self,
        samples: Iterable[MoistureSample],
    ) -> None:
        """
        Add multiple existing samples.
        """

        for sample in samples:

            self._samples.append(
                sample
            )