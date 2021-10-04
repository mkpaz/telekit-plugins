package telekit.plugins.linetest.provider.impl;

import org.junit.jupiter.api.Test;
import telekit.plugins.linetest.domain.LineStatus;
import telekit.plugins.linetest.domain.Measurement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static telekit.plugins.linetest.domain.MeasuredValue.ValueType;
import static telekit.plugins.linetest.provider.impl.IskratelSAKOutputParser.cleanupOutput;

public class IskratelSAKOutputParserTest {

    @Test
    public void testCleanupOutput() {
        assertThat(cleanupOutput(CMD_ESCAPE_SEQUENCE)).isEqualTo("start_test 1 A4 1");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testParseLineParamsSWA033() {
        Measurement measurement = new IskratelSAKOutputParser().parse(MASK_MYHM0A33_NORMAL);

        assertFalse(measurement.isTestFailed());
        assertEquals(measurement.getLineStatus(), LineStatus.ON_HOOK);
        assertEquals(-0.278, measurement.getMeasuredValue(ValueType.AC_VOLTAGE).getTipRing(), 0.0);
        assertEquals(0, measurement.getMeasuredValue(ValueType.DC_VOLTAGE).getTipRing(), 0.0);
        assertEquals(10e6, measurement.getMeasuredValue(ValueType.RESISTANCE).getTipRing(), 0.0);
        assertEquals(0.159e-9, measurement.getMeasuredValue(ValueType.CAPACITANCE).getTipRing(), 1e-15);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testParseLineParamsSWA60() {
        Measurement measurement = new IskratelSAKOutputParser().parse(MASK_MYHM0A60_NORMAL);

        assertFalse(measurement.isTestFailed());
        assertEquals(measurement.getLineStatus(), LineStatus.ON_HOOK);
        assertEquals(0.244, measurement.getMeasuredValue(ValueType.AC_VOLTAGE).getTipRing(), 0.0);
        assertEquals(0.017, measurement.getMeasuredValue(ValueType.DC_VOLTAGE).getTipRing(), 0.0);
        assertEquals(10e6, measurement.getMeasuredValue(ValueType.RESISTANCE).getTipRing(), 0.0);
        assertEquals(0.382e-9, measurement.getMeasuredValue(ValueType.CAPACITANCE).getTipRing(), 1e-15);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testParseLineParamsSWA08() {
        Measurement measurement = new IskratelSAKOutputParser().parse(MASK_MYHM0A08_NORMAL);

        assertFalse(measurement.isTestFailed());
        assertEquals(measurement.getLineStatus(), LineStatus.ON_HOOK);
        assertEquals(0.270, measurement.getMeasuredValue(ValueType.AC_VOLTAGE).getTipRing(), 0.0);
        assertEquals(0.1, measurement.getMeasuredValue(ValueType.DC_VOLTAGE).getTipRing(), 0.0);
        assertEquals(10e6, measurement.getMeasuredValue(ValueType.RESISTANCE).getTipRing(), 0.0);
        assertEquals(284e-9, measurement.getMeasuredValue(ValueType.CAPACITANCE).getTipRing(), 1e-15);
    }

    @Test
    public void testParseLineParamsOutOfTolerance() {
        Measurement measurement = new IskratelSAKOutputParser().parse(MASK_OUT_OF_TOLERANCE);

        assertTrue(measurement.isTestFailed());
        assertEquals(measurement.getLineStatus(), LineStatus.UNKNOWN);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testParseLineParamsNonStandardValues() {
        Measurement measurement = new IskratelSAKOutputParser().parse(MASK_NON_STANDARD_VALUES);

        assertFalse(measurement.isTestFailed());
        assertEquals(measurement.getLineStatus(), LineStatus.ON_HOOK);
        assertEquals(0.0, measurement.getMeasuredValue(ValueType.AC_VOLTAGE).getTipRing(), 0.0);
        assertEquals(0.0, measurement.getMeasuredValue(ValueType.DC_VOLTAGE).getTipRing(), 0.0);
        assertEquals(10e6, measurement.getMeasuredValue(ValueType.RESISTANCE).getTipRing(), 0.0);
        assertEquals(0.0, measurement.getMeasuredValue(ValueType.RESISTANCE).getRingGround(), 0.0);
        assertEquals(0.0, measurement.getMeasuredValue(ValueType.CAPACITANCE).getTipRing(), 0.0);
    }

    @Test
    public void testParseLineParamsBusyState() {
        Measurement measurement = new IskratelSAKOutputParser().parse(MASK_BUSY);

        assertTrue(measurement.isTestFailed());
        assertEquals(measurement.getLineStatus(), LineStatus.CONNECTED);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testParseLineParamsLineParking() {
        Measurement measurement = new IskratelSAKOutputParser().parse(MASK_LINE_PARKING);

        assertFalse(measurement.isTestFailed());
        assertEquals(measurement.getLineStatus(), LineStatus.OFF_HOOK);
        assertEquals(4.5, measurement.getMeasuredValue(ValueType.AC_VOLTAGE).getTipRing(), 0.0);
        assertEquals(-42.5, measurement.getMeasuredValue(ValueType.DC_VOLTAGE).getTipRing(), 0.0);
        assertEquals(3778.2, measurement.getMeasuredValue(ValueType.RESISTANCE).getTipRing(), 0.0);
        assertEquals(0.000, measurement.getMeasuredValue(ValueType.CAPACITANCE).getTipRing(), 0.0);
    }

    private static final String MASK_MYHM0A33_NORMAL = """
            Please wait!
            => Results for test A4:
                            
            Measurement with ON-HOOK!
                            
            Voltages:
                    Uab(~):   -0.278 V
                    Uab(=):   0.000 V
                    Uag(~):   0.332 V
                    Uag(=):   0.292 V
                    Ubg(~):   0.036 V
                    Ubg(=):   0.297 V
                            
            Resistances:
                    Rab:   10000000 (Ohm)
                    Rag:   10000000 (Ohm)
                    Rbg:   10000000 (Ohm)
                            
            Capacities:
                    Cab:   0.159 (nF)
                    Cag:   0.000 (nF)
                    Cbg:   0.000 (nF)
                            
            Press ENTER !
            """;

    private static final String MASK_MYHM0A60_NORMAL = """
            Please wait!
            => Results for test A4:
                            
            Measurement with ON-HOOK!
                            
            Voltages:
                    Uab(~):   0.244 V
                    Uab(=):   0.017 V
                    Uag(~):   0.222 V
                    Uag(=):   0.316 V
                    Ubg(~):   0.048 V
                    Ubg(=):   0.302 V
                            
            Resistances:
                    Rab:   10000000 (Ohm)
                    Rag:   10000000 (Ohm)
                    Rbg:   10000000 (Ohm)
                            
            Capacities:
                    Cab:   0.382 (nF)
                    Cag:   0.000 (nF)
                    Cbg:   0.000 (nF)
                            
            Press ENTER !
            """;

    private static final String MASK_MYHM0A08_NORMAL = """
            Please wait!
            => Results for test A4:
                            
            Measurement with ON-HOOK!
                            
            Voltages:
                    Uab(~):   0.270 V
                    Uab(=):   < 0.1 V
                    Uag(~):   0.245 V
                    Uag(=):   0.196 V
                    Ubg(~):   < 0.1 V
                    Ubg(=):   0.212 V
                            
            Resistances:
                    Rab:   > 5000 KE
                    Rag:   > 5000 KE
                    Rbg:   > 5000 KE
                            
            Capacities:
                    Cab:   284 nF
                    Cag:   < 10 nF
                    Cbg:   < 10 nF
                            
            Press ENTER !
            """;

    private static final String MASK_OUT_OF_TOLERANCE = """
            Please wait!
            => Results for test A4:
                            
            Measurement with ON-HOOK!
                            
            Voltages:
                    Uab:   0.0 (V~)
                    Uab:   0.0 (V=)
                    Uag:   0.0 (V~)
                    Uag:   0.0 (V=)
                    Ubg:   0.0 (V~)
                    Ubg:   0.0 (V=)
                            
            Resistances:
                    Rab:   0.0 (Ohm)  Out Of Tolerance (10-20)!
                    Rag:   < 10 KE
                    Rbg:   < 10 KE
                            
            Capacities:
                    Cab:   0.000 (nF)
                    Cag:   0.000 (nF)
                    Cbg:   0.000 (nF)
                            
            Press ENTER !
                            
            =>
            Press ENTER !
            """;

    private static final String MASK_NON_STANDARD_VALUES = """
            Please wait!
            => Results for test A4:
                            
            Measurement with ON-HOOK!
                            
            Voltages:
                    Uab:   -0.0 (V~)
                    Uab:   0.0 (V=)
                    Uag:   0.0 (V~)
                    Uag:   0.0 (V=)
                    Ubg:   0.0 (V~)
                    Ubg:   0.0 (V=)
                            
            Resistances:
                    Rab:   > 1000 KE
                    Rag:   0 (Ohm)
                    Rbg:   < 10 KE
                            
            Capacities:
                    Cab:   < 10 nF
                    Cag:   0.000 (nF)
                    Cbg:   0.000 (nF)
                            
            Press ENTER !
                            
            =>
            Press ENTER !
            """;

    private static final String MASK_BUSY = """
            Please wait!
            => Measurement not possible for test A4!
            Line is in active mode!
            """;

    private static final String MASK_LINE_PARKING = """
            Please wait!
            => Results for test A4:
                            
            Measurement with OFF-HOOK - Line parking!
                            
            Voltages:
                    Uab:   4.5 (V~)
                    Uab:   -42.5 (V=)
                    Uag:   13.5 (V~)
                    Uag:   -45.8 (V=)
                    Ubg:   9.3 (V~)
                    Ubg:   -3.2 (V=)
                            
            Resistances:
                    Rab:   3778.2 (Ohm)
                    Rag:   7265.5 (Ohm)
                    Rbg:   4217.5 (Ohm)
                            
            Capacities:
                    Cab:   0.000 (nF)
                    Cag:   0.000 (nF)
                    Cbg:   0.000 (nF)
                            
            Press ENTER !
            """;

    private static final String CMD_ESCAPE_SEQUENCE =
            "7s8[1C7t8[1C7a8[1C7r8[1C7t8[1C7_8[1C7t8[1C7e8[1C7s8[1C7t8[1C7 8[1C718[1C7 8[1C7A8[1C748[1C7 8[1C718[1C";
}