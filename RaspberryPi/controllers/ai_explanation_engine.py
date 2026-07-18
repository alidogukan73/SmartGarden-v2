"""
AI explanation engine.
"""

from __future__ import annotations

from datetime import datetime

from models.ai_decision_summary import (
    AIDecisionSummary,
)
from models.ai_explanation import (
    AIExplanation,
)
from models.soil_learning_profile import (
    SoilLearningProfile,
)


class AIExplanationEngine:
    """
    Convert technical AI decisions into
    user-friendly explanations.

    Observation mode only.

    This engine never modifies irrigation commands
    and never controls the relay directly.
    """

    def analyze(
        self,
        *,
        decision: AIDecisionSummary,
        soil_profile: SoilLearningProfile,
    ) -> AIExplanation:
        """
        Produce one user-friendly explanation.
        """

        if decision.decision_code == "LEARNING":

            return self._learning(
                decision=decision,
                profile=soil_profile,
            )

        if decision.decision_code == "SENSOR_UNSTABLE":

            return self._sensor_unstable(
                decision=decision,
            )

        if decision.decision_code == "SYSTEM_DISABLED":

            return self._system_disabled(
                decision=decision,
            )

        if decision.decision_code == "MANUAL_MODE":

            return self._manual_mode(
                decision=decision,
            )

        return self._healthy(
            decision=decision,
        )

    def _learning(
        self,
        *,
        decision: AIDecisionSummary,
        profile: SoilLearningProfile,
    ) -> AIExplanation:
        """
        Explain the current soil-learning progress.
        """

        remaining_sensor_samples = max(
            0,
            profile.remaining_sensor_samples,
        )

        remaining_auto_waterings = max(
            0,
            profile.remaining_auto_waterings,
        )

        progress = max(
            0,
            min(
                profile.learning_stage * 20,
                100,
            ),
        )

        reason_lines = [
            (
                f"{profile.sensor_history_count} "
                "sensör kaydı toplandı."
            ),
            (
                f"{profile.watering_count_analyzed} "
                "otomatik sulama analiz edildi."
            ),
            profile.next_milestone_text,
        ]

        if remaining_sensor_samples > 0:

            next_step = (
                f"{remaining_sensor_samples} sensör "
                "ölçümü daha gerekiyor."
            )

        elif remaining_auto_waterings > 0:

            next_step = (
                f"{remaining_auto_waterings} otomatik "
                "sulama daha gerekiyor."
            )

        else:

            next_step = (
                "Toprak davranışının güvenilir hale "
                "gelmesi bekleniyor."
            )

        return AIExplanation(
            explanation_code="LEARNING_PROGRESS",

            title=decision.decision_title,

            summary=(
                "Toprak davranışı analiz edilerek "
                "en uygun sulama stratejisi oluşturuluyor."
            ),

            reason_lines=tuple(
                reason_lines
            ),

            next_step=next_step,

            progress_percent=progress,

            severity="INFO",

            generated_at=datetime.now().isoformat(),
        )

    def _sensor_unstable(
        self,
        *,
        decision: AIDecisionSummary,
    ) -> AIExplanation:
        """
        Explain why unstable sensor readings block decisions.
        """

        return AIExplanation(
            explanation_code="SENSOR_WARNING",

            title=decision.decision_title,

            summary=(
                "Kararsız ölçümler nedeniyle "
                "sulama kararı güvenlik amacıyla ertelendi."
            ),

            reason_lines=(
                "Sensör ölçümleri kararlı değil.",
                "Yeni ve tutarlı ölçümler bekleniyor.",
                (
                    "Pompa, güvenilir sensör verisi "
                    "oluşmadan otomatik başlatılmayacak."
                ),
            ),

            next_step=(
                "Sensör kararlı olduğunda analiz "
                "otomatik olarak devam edecek."
            ),

            progress_percent=0,

            severity="WARNING",

            generated_at=datetime.now().isoformat(),
        )

    def _system_disabled(
        self,
        *,
        decision: AIDecisionSummary,
    ) -> AIExplanation:
        """
        Explain the disabled-system state.
        """

        return AIExplanation(
            explanation_code="SYSTEM_DISABLED",

            title=decision.decision_title,

            summary=(
                "Sulama sistemi kullanıcı ayarlarından "
                "devre dışı bırakılmış durumda."
            ),

            reason_lines=(
                "Otomatik sulama kararları uygulanmıyor.",
                "Pompa güvenlik amacıyla kapalı tutuluyor.",
            ),

            next_step=(
                "Sulama işlemlerini yeniden başlatmak için "
                "sistemi Ayarlar ekranından etkinleştirin."
            ),

            progress_percent=100,

            severity="INFO",

            generated_at=datetime.now().isoformat(),
        )

    def _manual_mode(
        self,
        *,
        decision: AIDecisionSummary,
    ) -> AIExplanation:
        """
        Explain the manual-control state.
        """

        return AIExplanation(
            explanation_code="MANUAL_MODE",

            title=decision.decision_title,

            summary=(
                "Otomatik karar sistemi izleme yapmaya "
                "devam ediyor ancak pompa kullanıcı "
                "tarafından yönetiliyor."
            ),

            reason_lines=(
                "Otomatik sulama modu kapalı.",
                "Pompa komutları manuel olarak uygulanıyor.",
                (
                    "Öğrenme motorları sensör ve sulama "
                    "verilerini toplamaya devam ediyor."
                ),
            ),

            next_step=(
                "Otomatik kararları etkinleştirmek için "
                "otomatik sulama modunu açın."
            ),

            progress_percent=100,

            severity="INFO",

            generated_at=datetime.now().isoformat(),
        )

    def _healthy(
        self,
        *,
        decision: AIDecisionSummary,
    ) -> AIExplanation:
        """
        Explain normal, watering and optimization decisions.
        """

        if decision.decision_code == "WATERING_RECOMMENDED":

            return AIExplanation(
                explanation_code="WATERING_REQUIRED",

                title=decision.decision_title,

                summary=(
                    "Toprak nemi belirlenen sınırın altında "
                    "ve mevcut veriler sulama ihtiyacını "
                    "destekliyor."
                ),

                reason_lines=(
                    (
                        "Sensör ölçümleri sulama kararı "
                        "için yeterince kararlı."
                    ),
                    (
                        "Toprak nemi tanımlanan sınırın "
                        "altında bulunuyor."
                    ),
                    (
                        "Toprak davranışı ve geçmiş veriler "
                        "kararla birlikte değerlendirildi."
                    ),
                ),

                next_step=(
                    "Otomatik mod etkinse sulama işlemi "
                    "güvenlik kurallarıyla başlatılabilir."
                ),

                progress_percent=100,

                severity="WARNING",

                generated_at=datetime.now().isoformat(),
            )

        if decision.decision_code == "INCREASE_PUMP_DURATION":

            return AIExplanation(
                explanation_code=(
                    "PUMP_INCREASE_RECOMMENDED"
                ),

                title=decision.decision_title,

                summary=(
                    "Geçmiş sulamalarda elde edilen ortalama "
                    "nem artışı hedeflenen seviyenin altında."
                ),

                reason_lines=(
                    (
                        "Tamamlanmış otomatik sulamalar "
                        "analiz edildi."
                    ),
                    (
                        "Sulama sonrası ortalama nem kazancı "
                        "düşük bulundu."
                    ),
                    (
                        "Öneri yalnızca gözlem amaçlıdır ve "
                        "ayarları otomatik değiştirmez."
                    ),
                ),

                next_step=(
                    "Pompa süresi önerisini Android "
                    "uygulamasından inceleyin."
                ),

                progress_percent=100,

                severity="WARNING",

                generated_at=datetime.now().isoformat(),
            )

        if decision.decision_code == "DECREASE_PUMP_DURATION":

            return AIExplanation(
                explanation_code=(
                    "PUMP_DECREASE_RECOMMENDED"
                ),

                title=decision.decision_title,

                summary=(
                    "Geçmiş sulamalarda elde edilen nem "
                    "artışı yüksek olduğu için daha kısa "
                    "pompa süresi yeterli olabilir."
                ),

                reason_lines=(
                    (
                        "Sulama sonrası nem artışı hedef "
                        "aralığın üzerinde bulundu."
                    ),
                    (
                        "Daha kısa çalışma süresi su ve "
                        "enerji tasarrufu sağlayabilir."
                    ),
                    (
                        "Öneri güvenlik amacıyla otomatik "
                        "olarak uygulanmıyor."
                    ),
                ),

                next_step=(
                    "Pompa süresi önerisini Android "
                    "uygulamasından değerlendirin."
                ),

                progress_percent=100,

                severity="INFO",

                generated_at=datetime.now().isoformat(),
            )

        return AIExplanation(
            explanation_code="SYSTEM_HEALTHY",

            title=decision.decision_title,

            summary=decision.decision_message,

            reason_lines=(
                (
                    "Toprak nemi mevcut ayarlarla "
                    "uyumlu görünüyor."
                ),
                (
                    "Geçmiş sulama sonuçları ek bir "
                    "değişiklik gerektirmiyor."
                ),
                (
                    "Sistem sensör verilerini izlemeye "
                    "ve öğrenmeye devam ediyor."
                ),
            ),

            next_step=(
                "Şu anda kullanıcı müdahalesi gerekmiyor."
            ),

            progress_percent=100,

            severity=decision.severity,

            generated_at=datetime.now().isoformat(),
        )