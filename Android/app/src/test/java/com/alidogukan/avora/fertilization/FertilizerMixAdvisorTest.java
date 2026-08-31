package com.alidogukan.avora.fertilization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.alidogukan.avora.models.FertilizerProduct;

import org.junit.Test;

import java.util.Arrays;

public class FertilizerMixAdvisorTest {

    @Test
    public void sameProductIsBlocked() {
        FertilizerProduct product = product("same", "20-20-20", "20-20-20");

        FertilizerMixResult result = FertilizerMixAdvisor.assess(
                product,
                product
        );

        assertTrue(result.isBlocked());
        assertEquals(
                FertilizerMixResult.RiskLevel.BLOCKED,
                result.getRiskLevel()
        );
    }

    @Test
    public void calciumAndPhosphateAreBlocked() {
        FertilizerMixResult result = FertilizerMixAdvisor.assess(
                product("calcium", "Calsimagsi", "Kalsiyum"),
                product("phosphate", "10-5-40", "10-5-40")
        );

        assertTrue(result.isBlocked());
    }

    @Test
    public void calciumAndSulphateAreBlocked() {
        FertilizerMixResult result = FertilizerMixAdvisor.assess(
                product("calcium", "Kalsiyum desteği", "Calcium"),
                product("sulphate", "MAGSUL", "Magnezyum sülfat")
        );

        assertTrue(result.isBlocked());
    }

    @Test
    public void calciumWithUnclassifiedProductRequiresCaution() {
        FertilizerMixResult result = FertilizerMixAdvisor.assess(
                product("calcium", "Kalsiyum desteği", "Calcium"),
                product("seaweed", "Deniz yosunu", "")
        );

        assertFalse(result.isBlocked());
        assertTrue(result.requiresConfirmation());
        assertEquals(
                FertilizerMixResult.RiskLevel.CAUTION,
                result.getRiskLevel()
        );
    }

    @Test
    public void unknownPairRequiresExplicitConfirmation() {
        FertilizerMixResult result = FertilizerMixAdvisor.assess(
                product("a", "Deniz yosunu", ""),
                product("b", "Hümik fülvik", "")
        );

        assertFalse(result.isBlocked());
        assertTrue(result.requiresConfirmation());
        assertEquals(
                FertilizerMixResult.RiskLevel.UNVERIFIED,
                result.getRiskLevel()
        );
    }

    @Test
    public void structuredCalciumAndPhosphateTagsAreBlocked() {
        FertilizerProduct calcium = product("calcium", "Destek A", "");
        calcium.setFunctional_tags(Arrays.asList("CALCIUM"));
        FertilizerProduct phosphate = product("phosphate", "Destek B", "");
        phosphate.setFunctional_tags(Arrays.asList("PHOSPHATE"));

        FertilizerMixResult result = FertilizerMixAdvisor.assess(
                calcium,
                phosphate
        );

        assertTrue(result.isBlocked());
    }

    @Test
    public void twoBalancedNpkProductsAreBlocked() {
        FertilizerMixResult result = FertilizerMixAdvisor.assess(
                product("first", "Dengeli A", "20-20-20"),
                product("second", "Dengeli B", "18-18-18")
        );

        assertTrue(result.isBlocked());
    }

    @Test
    public void microbialAndMineralProductRequireCaution() {
        FertilizerProduct microbial = product("microbial", "Canlı destek", "");
        microbial.setFunctional_tags(Arrays.asList("MICROBIAL"));

        FertilizerMixResult result = FertilizerMixAdvisor.assess(
                microbial,
                product("mineral", "Ana besleme", "10-5-40")
        );

        assertFalse(result.isBlocked());
        assertEquals(
                FertilizerMixResult.RiskLevel.CAUTION,
                result.getRiskLevel()
        );
    }

    @Test
    public void humicAndMineralProductRequireCaution() {
        FertilizerProduct humic = product("humic", "Toprak desteği", "");
        humic.setFunctional_tags(Arrays.asList("HUMIC_FULVIC"));

        FertilizerMixResult result = FertilizerMixAdvisor.assess(
                humic,
                product("mineral", "Ana besleme", "20-20-20")
        );

        assertFalse(result.isBlocked());
        assertEquals(
                FertilizerMixResult.RiskLevel.CAUTION,
                result.getRiskLevel()
        );
    }

    private FertilizerProduct product(
            String id,
            String name,
            String npk
    ) {
        FertilizerProduct product = new FertilizerProduct();
        product.setProduct_id(id);
        product.setName(name);
        product.setNpk(npk);
        return product;
    }
}