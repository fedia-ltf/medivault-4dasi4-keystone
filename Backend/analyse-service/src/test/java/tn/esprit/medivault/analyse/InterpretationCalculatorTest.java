package tn.esprit.medivault.analyse;

import org.junit.jupiter.api.Test;
import tn.esprit.medivault.analyse.enums.Interpretation;
import tn.esprit.medivault.analyse.service.InterpretationCalculator;

import static org.assertj.core.api.Assertions.assertThat;

class InterpretationCalculatorTest {

    @Test
    void interpretation_selonIntervalleDeReference() {
        assertThat(InterpretationCalculator.interpreter(1.30, 0.70, 1.10)).isEqualTo(Interpretation.ELEVE);
        assertThat(InterpretationCalculator.interpreter(0.50, 0.70, 1.10)).isEqualTo(Interpretation.BAS);
        assertThat(InterpretationCalculator.interpreter(0.90, 0.70, 1.10)).isEqualTo(Interpretation.NORMAL);
        assertThat(InterpretationCalculator.interpreter(1.10, 0.70, 1.10)).isEqualTo(Interpretation.NORMAL);
    }
}
