package fr.mangi.zendure

import fr.mangi.zendure.model.ZendureModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZendureModelTest {

    @Test
    fun `hyper 2000 est identifie par le prefixe zene`() {
        assertEquals(ZendureModel.HYPER_2000, ZendureModel.fromBleName("ZenE4_EF"))
        assertEquals(ZendureModel.HYPER_2000, ZendureModel.fromBleName("zene123"))
    }

    @Test
    fun `tous les prefixes de modeles sont resolus`() {
        assertEquals(ZendureModel.HUB_1200, ZendureModel.fromBleName("ZenP1_38"))
        assertEquals(ZendureModel.HUB_2000, ZendureModel.fromBleName("ZenH2_AB"))
        assertEquals(ZendureModel.AIO_2400, ZendureModel.fromBleName("ZenR3_CD"))
        assertEquals(ZendureModel.ACE_1500, ZendureModel.fromBleName("ZenF5_GH"))
    }

    @Test
    fun `un nom zen inconnu est zendure mais sans modele`() {
        assertTrue(ZendureModel.isZendure("ZenX99"))
        assertNull(ZendureModel.fromBleName("ZenX99"))
    }

    @Test
    fun `les autres noms ne sont pas zendure`() {
        assertFalse(ZendureModel.isZendure("Hue Lamp"))
        assertFalse(ZendureModel.isZendure(null))
        assertNull(ZendureModel.fromBleName(null))
    }

    @Test
    fun `productKey du hyper 2000`() {
        assertEquals("ja72U0ha", ZendureModel.HYPER_2000.productKey)
    }
}
