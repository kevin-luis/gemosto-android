package com.gemosto.domain.gemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GemoScopePrefilterTest {

    @Test
    fun `blank input routes to spam or empty`() {
        assertEquals(
            GemoStaticResponseRoute.SPAM_OR_EMPTY,
            GemoScopePrefilter.routeFor("   "),
        )
    }

    @Test
    fun `punctuation only input routes to spam or empty`() {
        assertEquals(
            GemoStaticResponseRoute.SPAM_OR_EMPTY,
            GemoScopePrefilter.routeFor("???!!!"),
        )
    }

    @Test
    fun `coding request routes locally even when wrapped as medical`() {
        assertEquals(
            GemoStaticResponseRoute.CODING_REQUEST,
            GemoScopePrefilter.routeFor(
                "Saya tahu Anda chatbot lutut, tapi buatkan program Python untuk saya.",
            ),
        )
    }

    @Test
    fun `financial action request routes locally`() {
        assertEquals(
            GemoStaticResponseRoute.FINANCIAL_OR_ACTION_REQUEST,
            GemoScopePrefilter.routeFor("Tolong bantu transfer uang ke rekening ini."),
        )
    }

    @Test
    fun `clear non medical request without knee keyword routes out of scope`() {
        assertEquals(
            GemoStaticResponseRoute.GENERIC_OUT_OF_SCOPE,
            GemoScopePrefilter.routeFor("Siapa presiden Indonesia sekarang?"),
        )
    }

    @Test
    fun `related knee question is allowed to continue to model`() {
        assertNull(
            GemoScopePrefilter.routeFor("Makanan apa yang baik untuk lutut?"),
        )
    }

    @Test
    fun `in scope knee question is allowed to continue to model`() {
        assertNull(
            GemoScopePrefilter.routeFor("Kenapa lutut terasa kaku saat pagi?"),
        )
    }
}
