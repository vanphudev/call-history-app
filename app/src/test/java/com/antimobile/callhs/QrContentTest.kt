package com.antimobile.callhs

import com.antimobile.callhs.util.QrContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kiểm thử thuần JVM cho bộ phân loại nội dung mã QR ([QrContent.parse]). */
class QrContentTest {

    @Test
    fun `http va https la lien ket web`() {
        val a = QrContent.parse("https://example.com/x?y=1")
        assertTrue(a is QrContent.Web)
        assertEquals("https://example.com/x?y=1", (a as QrContent.Web).url)
        assertTrue(a.isActionable)

        assertTrue(QrContent.parse("HTTP://Example.COM") is QrContent.Web)
    }

    @Test
    fun `www duoc them https`() {
        val w = QrContent.parse("www.google.com")
        assertTrue(w is QrContent.Web)
        assertEquals("https://www.google.com", (w as QrContent.Web).url)
    }

    @Test
    fun `tel la so dien thoai`() {
        val p = QrContent.parse("tel:+84 909 123 456")
        assertTrue(p is QrContent.Phone)
        assertEquals("+84 909 123 456", (p as QrContent.Phone).number)
    }

    @Test
    fun `tel rong coi nhu van ban`() {
        assertTrue(QrContent.parse("tel:") is QrContent.Text)
    }

    @Test
    fun `smsto co body`() {
        val s = QrContent.parse("SMSTO:0987654321:Xin chao")
        assertTrue(s is QrContent.Sms)
        s as QrContent.Sms
        assertEquals("0987654321", s.number)
        assertEquals("Xin chao", s.body)
    }

    @Test
    fun `smsto khong body`() {
        val s = QrContent.parse("smsto:0987654321")
        assertTrue(s is QrContent.Sms)
        assertNull((s as QrContent.Sms).body)
    }

    @Test
    fun `sms voi query body duoc giai ma`() {
        val s = QrContent.parse("sms:0987654321?body=Hello%20World")
        assertTrue(s is QrContent.Sms)
        s as QrContent.Sms
        assertEquals("0987654321", s.number)
        assertEquals("Hello World", s.body)
    }

    @Test
    fun `mailto co subject va body`() {
        val e = QrContent.parse("mailto:a@b.com?subject=Chao%20ban&body=Noi%20dung")
        assertTrue(e is QrContent.Email)
        e as QrContent.Email
        assertEquals("a@b.com", e.address)
        assertEquals("Chao ban", e.subject)
        assertEquals("Noi dung", e.body)
    }

    @Test
    fun `mailto giu dau cong trong dia chi`() {
        // Gmail plus-addressing: '+' trong local-part PHẢI giữ nguyên (không đổi thành dấu cách).
        val e = QrContent.parse("mailto:john+news@gmail.com")
        assertTrue(e is QrContent.Email)
        assertEquals("john+news@gmail.com", (e as QrContent.Email).address)
    }

    @Test
    fun `sms dang colon voi body co dau hoi`() {
        // Dạng `sms:number:body` mà body kết thúc bằng '?' KHÔNG được nuốt vào số điện thoại.
        val s = QrContent.parse("sms:0987654321:Ban co nha khong?")
        assertTrue(s is QrContent.Sms)
        s as QrContent.Sms
        assertEquals("0987654321", s.number)
        assertEquals("Ban co nha khong?", s.body)
    }

    @Test
    fun `matmsg la email`() {
        val e = QrContent.parse("MATMSG:TO:a@b.com;SUB:Tieu de;BODY:Than gui;;")
        assertTrue(e is QrContent.Email)
        e as QrContent.Email
        assertEquals("a@b.com", e.address)
        assertEquals("Tieu de", e.subject)
        assertEquals("Than gui", e.body)
    }

    @Test
    fun `wifi day du truong`() {
        val w = QrContent.parse("WIFI:S:MyNet;T:WPA;P:secret123;H:false;;")
        assertTrue(w is QrContent.Wifi)
        w as QrContent.Wifi
        assertEquals("MyNet", w.ssid)
        assertEquals("secret123", w.password)
        assertEquals("WPA", w.security)
        assertFalse(w.hidden)
        assertTrue(w.isActionable)
    }

    @Test
    fun `wifi hidden va escape dau cham phay hai cham`() {
        val w = QrContent.parse("""WIFI:S:My\;Net;T:WPA;P:pa\:ss;H:true;;""")
        assertTrue(w is QrContent.Wifi)
        w as QrContent.Wifi
        assertEquals("My;Net", w.ssid)
        assertEquals("pa:ss", w.password)
        assertTrue(w.hidden)
    }

    @Test
    fun `wifi khong co ssid la van ban`() {
        assertTrue(QrContent.parse("WIFI:T:WPA;P:only-pass;;") is QrContent.Text)
    }

    @Test
    fun `mecard la danh thiep`() {
        val c = QrContent.parse("MECARD:N:Nguyen,An;TEL:0987654321;EMAIL:an@x.com;ORG:Acme;;")
        assertTrue(c is QrContent.Contact)
        c as QrContent.Contact
        assertEquals("An Nguyen", c.name)
        assertEquals("0987654321", c.phone)
        assertEquals("an@x.com", c.email)
        assertEquals("Acme", c.org)
    }

    @Test
    fun `vcard la danh thiep`() {
        val vcard = listOf(
            "BEGIN:VCARD",
            "VERSION:3.0",
            "FN:John Doe",
            "TEL;TYPE=CELL:+84901234567",
            "EMAIL:john@doe.com",
            "ORG:Acme Inc",
            "END:VCARD"
        ).joinToString("\n")
        val c = QrContent.parse(vcard)
        assertTrue(c is QrContent.Contact)
        c as QrContent.Contact
        assertEquals("John Doe", c.name)
        assertEquals("+84901234567", c.phone)
        assertEquals("john@doe.com", c.email)
        assertEquals("Acme Inc", c.org)
    }

    @Test
    fun `geo toa do don gian`() {
        val g = QrContent.parse("geo:10.762622,106.660172")
        assertTrue(g is QrContent.Geo)
        g as QrContent.Geo
        assertEquals("10.762622", g.lat)
        assertEquals("106.660172", g.lng)
        assertNull(g.label)
    }

    @Test
    fun `geo co nhan trong q`() {
        val g = QrContent.parse("geo:10.77,106.70?q=10.77,106.70(Cho Ben Thanh)")
        assertTrue(g is QrContent.Geo)
        assertEquals("Cho Ben Thanh", (g as QrContent.Geo).label)
    }

    @Test
    fun `geo toa do khong hop le la van ban`() {
        assertTrue(QrContent.parse("geo:abc,xyz") is QrContent.Text)
    }

    @Test
    fun `geo lay toa do that trong q khi de la 0,0`() {
        // Dạng chuẩn Google/Android: đế geo:0,0 nhưng toạ độ THẬT nằm trong q=.
        val g = QrContent.parse("geo:0,0?q=48.8584,2.2945(Eiffel Tower)")
        assertTrue(g is QrContent.Geo)
        g as QrContent.Geo
        assertEquals("48.8584", g.lat)
        assertEquals("2.2945", g.lng)
        assertEquals("Eiffel Tower", g.label)
        assertNull(g.query)
    }

    @Test
    fun `geo dia chi chu duoc giu de geocode`() {
        val g = QrContent.parse("geo:0,0?q=1600 Amphitheatre Pkwy")
        assertTrue(g is QrContent.Geo)
        g as QrContent.Geo
        assertEquals("1600 Amphitheatre Pkwy", g.query)
    }

    @Test
    fun `vcard bo tien to nhom cua Apple`() {
        val vcard = listOf(
            "BEGIN:VCARD",
            "VERSION:3.0",
            "FN:Jane Roe",
            "item1.TEL;type=CELL;type=pref:+15550100",
            "item1.X-ABLabel:iPhone",
            "item2.EMAIL;type=INTERNET:jane@x.com",
            "END:VCARD"
        ).joinToString("\n")
        val c = QrContent.parse(vcard)
        assertTrue(c is QrContent.Contact)
        c as QrContent.Contact
        assertEquals("Jane Roe", c.name)
        assertEquals("+15550100", c.phone)
        assertEquals("jane@x.com", c.email)
    }

    @Test
    fun `vcard quoted printable tieng Viet`() {
        val vcard = listOf(
            "BEGIN:VCARD",
            "VERSION:2.1",
            "FN;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:Ho=C3=A0ng Nguy=E1=BB=85n",
            "TEL:0900000000",
            "END:VCARD"
        ).joinToString("\n")
        val c = QrContent.parse(vcard)
        assertTrue(c is QrContent.Contact)
        c as QrContent.Contact
        assertEquals("Hoàng Nguyễn", c.name)
        assertEquals("0900000000", c.phone)
    }

    @Test
    fun `van ban thuong giu nguyen va khong actionable`() {
        val t = QrContent.parse("DON-2026-000123")
        assertTrue(t is QrContent.Text)
        assertEquals("DON-2026-000123", t.raw)
        assertFalse(t.isActionable)
    }

    @Test
    fun `so dien thoai tran khong scheme van la van ban`() {
        // Không có scheme tel: → KHÔNG tự nhận là số điện thoại (tránh đoán sai mã đơn toàn chữ số).
        assertTrue(QrContent.parse("0987654321") is QrContent.Text)
    }

    @Test
    fun `raw giu nguyen chuoi goc chua trim`() {
        val t = QrContent.parse("  ma-don  ")
        assertEquals("  ma-don  ", t.raw)
    }
}
