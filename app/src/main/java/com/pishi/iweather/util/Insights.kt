package com.pishi.iweather.util

import com.pishi.iweather.data.DayForecast
import com.pishi.iweather.data.SkyKind
import com.pishi.iweather.data.WeatherReport
import com.pishi.iweather.data.wmoInfo
import kotlin.math.roundToInt

data class ActivityScore(val title: String, val emoji: String, val score: Int, val note: String)
data class Insight(val emoji: String, val text: String)
data class AlertItem(val type: AlertType, val emoji: String, val text: String)

/** Rule-based, fully on-device "smart" layer. No AI call, no cost, works offline on cached data. Bilingual (FA/EN). */
object Insights {

    fun moodLine(report: WeatherReport, lang: Lang): String {
        val c = report.current
        val kind = wmoInfo(c.weatherCode, c.isDay).kind
        val fa = lang == Lang.FA
        return when {
            kind == SkyKind.CLEAR_DAY && c.temperature in 18.0..27.0 ->
                if (fa) "از اون روزهاست که حیفِ تو خونه موندنه؛ آسمون صاف و هوا دقیقاً به‌اندازه‌ست."
                else "One of those days that's wasted indoors; clear sky and just-right weather."
            kind == SkyKind.CLEAR_DAY && c.temperature > 32 ->
                if (fa) "آفتاب امروز جدی گرفته؛ سایه و آب، دو تا دوست صمیمی‌ت‌ان."
                else "The sun means business today; shade and water are your best friends."
            kind == SkyKind.CLEAR_NIGHT ->
                if (fa) "شبِ صاف و آروم؛ بهترین وقت برای یه پیاده‌روی کوتاه و نگاه به آسمون."
                else "A clear, calm night; perfect for a short walk and some stargazing."
            kind == SkyKind.RAIN && c.precipitation > 2 ->
                if (fa) "بارون حسابی می‌باره؛ صدای بارون رو مهمونِ امروز بدون."
                else "It's properly raining; let the sound of rain be today's guest."
            kind == SkyKind.RAIN ->
                if (fa) "یه بارون لطیف تو راهه یا در جریانه؛ هوا تازه و تمیز می‌شه."
                else "Gentle rain on the way or falling; the air will feel fresh and clean."
            kind == SkyKind.SNOW ->
                if (fa) "برف اومده که شهر رو سفیدپوش کنه؛ آروم برو، لذتش رو ببر."
                else "Snow is dressing the city in white; take it slow and enjoy it."
            kind == SkyKind.STORM ->
                if (fa) "آسمون امروز پر سر و صداست؛ کارهای بیرون رو بذار برای بعد از آروم شدنش."
                else "The sky is loud today; leave outdoor plans until it calms down."
            kind == SkyKind.FOG ->
                if (fa) "مه همه‌جا رو گرفته؛ با احتیاط و چراغ روشن حرکت کن."
                else "Fog everywhere; move carefully and keep your lights on."
            c.windSpeed > 30 ->
                if (fa) "باد امروز حرف اول رو می‌زنه؛ مواظب وسایل سبک تو بالکن باش."
                else "Wind runs the show today; watch light items on the balcony."
            else ->
                if (fa) "هوای امروز معمولی و بی‌دردسره؛ نه خیلی گرم، نه خیلی سرد."
                else "Ordinary, easy weather today; not too warm, not too cold."
        }
    }

    fun clothing(report: WeatherReport, st: AppSettings): Insight {
        val fa = st.isFa
        val t = report.current.feelsLike
        val rainy = report.hourly.take(6).any { it.precipProbability >= 50 }
        val base = when {
            t < 0 -> if (fa) "کاپشن ضخیم، کلاه و دستکش؛ سرما استخون‌سوزه." else "Heavy coat, hat and gloves; the cold bites to the bone."
            t < 8 -> if (fa) "یه کاپشن گرم بپوش؛ لایه‌لایه لباس پوشیدن بهترین کاره." else "Wear a warm coat; layering works best."
            t < 15 -> if (fa) "یه ژاکت یا پلیور کافیه؛ صبح و شب خنک‌تره." else "A jacket or sweater is enough; mornings and nights are cooler."
            t < 22 -> if (fa) "یه مانتو/پیراهن سبک با یه لایهٔ نازک رویی انتخاب خوبیه." else "A light shirt with a thin extra layer is a good pick."
            t < 30 -> if (fa) "لباس نخی و روشن بپوش؛ هوا مطبوعه." else "Cotton and light colours; the weather is pleasant."
            else -> if (fa) "سبک‌ترین لباس ممکن، کلاه آفتابی و حتماً یه بطری آب همراهت باشه." else "Lightest clothes possible, a sun hat, and definitely a bottle of water."
        }
        val umbrella = if (fa) " چتر هم بردار." else " Take an umbrella too."
        return Insight("👕", if (rainy) base + umbrella else base)
    }

    /** Probability-based rain window over the next 48h (start/end), from hourly forecast. */
    fun rainWindow(report: WeatherReport, st: AppSettings): Insight? {
        val fa = st.isFa
        val hours = report.hourly
        val startIdx = hours.indexOfFirst { it.precipProbability >= 50 }
        if (startIdx < 0) return null
        var endIdx = startIdx
        for (i in startIdx until hours.size) {
            if (hours[i].precipProbability >= 50) endIdx = i else if (i > endIdx + 1) break
        }
        val start = hours[startIdx]
        val end = hours[endIdx]
        val maxProb = hours.subList(startIdx, endIdx + 1).maxOf { it.precipProbability }
        val mm = hours.subList(startIdx, endIdx + 1).sumOf { it.precipitationMm }
        val startClock = Fmt.clock(start.hourLabel, st)
        val endClock = Fmt.clock(end.hourLabel, st)
        return if (fa) {
            val mmText = if (mm >= 0.5) "، مجموعاً حدود ${Fmt.precip(mm, st)}" else ""
            if (startIdx == 0) Insight("🌧️", "بارش محتمل است و احتمالاً تا ساعت $endClock ادامه دارد (اوج احتمال ${Fmt.percent(maxProb, st.lang)}$mmText).")
            else Insight("🌧️", "از ساعت $startClock بارش محتمل می‌شود و احتمالاً تا ساعت $endClock ادامه دارد (اوج احتمال ${Fmt.percent(maxProb, st.lang)}$mmText).")
        } else {
            val mmText = if (mm >= 0.5) ", about ${Fmt.precip(mm, st)} in total" else ""
            if (startIdx == 0) Insight("🌧️", "Rain is likely and may continue until $endClock (peak chance ${maxProb}%$mmText).")
            else Insight("🌧️", "Rain becomes likely from $startClock and may last until $endClock (peak chance ${maxProb}%$mmText).")
        }
    }

    /** Pressure trend from hourly surface pressure: now vs +6h. */
    fun pressureTrend(report: WeatherReport, lang: Lang): String {
        if (report.hourly.size < 7) return "—"
        val diff = report.hourly[6].pressure - report.hourly[0].pressure
        val fa = lang == Lang.FA
        return when {
            diff >= 1.5 -> if (fa) "رو به افزایش ↗ (هوا پایدارتر می‌شود)" else "Rising ↗ (more stable weather)"
            diff <= -1.5 -> if (fa) "رو به کاهش ↘ (نشانهٔ ناپایداری)" else "Falling ↘ (sign of instability)"
            else -> if (fa) "تقریباً ثابت" else "Steady"
        }
    }

    /** Health advice lines derived from the actual pollutant values. */
    fun healthAdvice(report: WeatherReport, lang: Lang): List<String> {
        val out = ArrayList<String>()
        val air = report.air ?: return out
        val fa = lang == Lang.FA
        air.usAqi?.let { aqi ->
            out += when {
                aqi <= 50 -> if (fa) "هوا پاکه؛ برای ورزش و فعالیت بیرون هیچ محدودیتی نیست." else "Air is clean; no limits on outdoor exercise or activity."
                aqi <= 100 -> if (fa) "کیفیت هوا قابل قبوله؛ فعالیت عادی بیرون مشکلی ندارد." else "Air quality is acceptable; normal outdoor activity is fine."
                aqi <= 150 -> if (fa) "گروه‌های حساس (کودکان، سالمندان، بیماران تنفسی) فعالیت سنگین و طولانی بیرون را کم کنند." else "Sensitive groups (children, elderly, respiratory patients) should reduce long, heavy outdoor activity."
                aqi <= 200 -> if (fa) "برای همه بهتر است فعالیت سنگین بیرون کم شود؛ گروه‌های حساس ماسک بزنند." else "Everyone should cut heavy outdoor activity; sensitive groups should wear a mask."
                else -> if (fa) "هوا ناسالم است؛ تا حد امکان بیرون نمانید و در صورت خروج ماسک مناسب (N95) بزنید." else "Air is unhealthy; stay indoors if you can, and wear a proper mask (N95) if you go out."
            }
        }
        air.pm10?.let { if (it >= 100) out += if (fa) "غلظت PM10 بالاست (${Fmt.num(it.roundToInt(), lang)})؛ نشانهٔ گردوغبار — پنجره‌ها را ببندید و ماسک بزنید." else "PM10 is high (${it.roundToInt()}); a dust sign — close windows and wear a mask." }
        air.dust?.let { if (it >= 35) out += if (fa) "گردوغبار در هوا زیاد است (${Fmt.num(it.roundToInt(), lang)})؛ دید کم می‌شود، با احتیاط رانندگی کنید." else "A lot of dust in the air (${it.roundToInt()}); visibility drops, drive carefully." }
        air.ozone?.let { if (it >= 160) out += if (fa) "ازن سطح زمین بالاست؛ ورزش سنگین را به صبح زود یا غروب موکول کنید." else "Ground-level ozone is high; move heavy exercise to early morning or evening." }
        return out
    }

    /** One-sentence generated summary of a forecast day, from its real numbers. */
    fun daySummary(day: DayForecast, st: AppSettings): String {
        val info = wmoInfo(day.weatherCode)
        val fa = st.isFa
        val label = if (fa) info.labelFa else info.labelEn
        val parts = ArrayList<String>()
        if (fa) {
            parts += "$label؛ بیشینه ${Fmt.temp(day.tempMax, st)} و کمینه ${Fmt.temp(day.tempMin, st)}"
            if (day.precipSumMm >= 0.5) parts += "مجموع بارش حدود ${Fmt.precip(day.precipSumMm, st)} (احتمال ${Fmt.percent(day.precipProbabilityMax, st.lang)})"
            else if (day.precipProbabilityMax >= 30) parts += "احتمال بارش ${Fmt.percent(day.precipProbabilityMax, st.lang)}"
            if (day.windGustsMax >= 50) parts += "تندباد تا ${Fmt.wind(day.windGustsMax, st)}"
            else if (day.windSpeedMax >= 25) parts += "باد تا ${Fmt.wind(day.windSpeedMax, st)} از سمت ${windDirection(day.windDirectionDominant, st.lang)}"
            if (day.uvMax >= 8) parts += "UV بالا (${Fmt.num(day.uvMax.roundToInt(), st.lang)})"
            return parts.joinToString("؛ ") + "."
        }
        parts += "$label; high ${Fmt.temp(day.tempMax, st)}, low ${Fmt.temp(day.tempMin, st)}"
        if (day.precipSumMm >= 0.5) parts += "total precipitation about ${Fmt.precip(day.precipSumMm, st)} (${day.precipProbabilityMax}% chance)"
        else if (day.precipProbabilityMax >= 30) parts += "${day.precipProbabilityMax}% chance of precipitation"
        if (day.windGustsMax >= 50) parts += "gusts up to ${Fmt.wind(day.windGustsMax, st)}"
        else if (day.windSpeedMax >= 25) parts += "wind up to ${Fmt.wind(day.windSpeedMax, st)} from the ${windDirection(day.windDirectionDominant, st.lang)}"
        if (day.uvMax >= 8) parts += "high UV (${day.uvMax.roundToInt()})"
        return parts.joinToString("; ") + "."
    }

    /** Typed, threshold-aware alerts for today/tomorrow. Types let notifications filter and dedupe. */
    fun alerts(report: WeatherReport, st: AppSettings, th: AlertThresholds = st.thresholds): List<AlertItem> {
        val fa = st.isFa
        val out = ArrayList<AlertItem>()
        val next12 = report.hourly.take(12)
        val maxPrecip = next12.maxOfOrNull { it.precipProbability } ?: 0
        if (maxPrecip >= 60) out += AlertItem(AlertType.RAIN, "☔",
            if (fa) "احتمال بارون تو ۱۲ ساعت آینده بالاست (${Fmt.percent(maxPrecip, st.lang)})؛ چتر یادت نره."
            else "High chance of rain in the next 12 hours (${maxPrecip}%); don't forget an umbrella.")
        else if (maxPrecip >= 35) out += AlertItem(AlertType.RAIN, "🌂",
            if (fa) "ممکنه بارون بیاد (${Fmt.percent(maxPrecip, st.lang)})؛ اگه مسیرت طولانیه، چتر بردار."
            else "It might rain (${maxPrecip}%); take an umbrella if your trip is long.")
        val today: DayForecast? = report.daily.firstOrNull()
        if (today != null && today.weatherCode in 95..99) out += AlertItem(AlertType.STORM, "⛈️",
            if (fa) "امروز رعدوبرق پیش‌بینی شده؛ هنگام رعد از فضای باز و زیر درخت دوری کن."
            else "Thunderstorms forecast today; avoid open spaces and trees during thunder.")
        if (today != null && today.uvMax >= 8) out += AlertItem(AlertType.UV, "🧴",
            if (fa) "شاخص UV امروز بالاست (${Fmt.num(today.uvMax.roundToInt(), st.lang)})؛ ضدآفتاب و عینک فراموش نشه."
            else "UV index is high today (${today.uvMax.roundToInt()}); don't forget sunscreen and sunglasses.")
        if (report.current.windSpeed >= 40) out += AlertItem(AlertType.WIND, "💨",
            if (fa) "باد شدیده (${Fmt.wind(report.current.windSpeed, st)})؛ مراقب باش."
            else "Strong wind right now (${Fmt.wind(report.current.windSpeed, st)}); be careful.")
        val maxGust = maxOf(report.current.windGusts, today?.windGustsMax ?: 0.0, next12.maxOfOrNull { it.windGusts } ?: 0.0)
        if (maxGust >= th.windGust) out += AlertItem(AlertType.WIND, "🌪️",
            if (fa) "تندباد شدید در راه است (تا ${Fmt.wind(maxGust, st)})؛ وسایل سبک را محکم کن."
            else "Severe gusts ahead (up to ${Fmt.wind(maxGust, st)}); secure light objects.")
        if (today != null && today.weatherCode in 71..86) out += AlertItem(AlertType.SNOW, "❄️",
            if (fa) "امروز بارش برف پیش‌بینی شده؛ زنجیرچرخ و زمان اضافه برای مسیر در نظر بگیر."
            else "Snowfall forecast today; consider snow chains and extra travel time.")
        if (today != null && today.tempMax >= th.tempHigh) out += AlertItem(AlertType.HEAT, "🔥",
            if (fa) "موج گرما: بیشینهٔ امروز ${Fmt.temp(today.tempMax, st)}؛ ظهر بیرون نمان و آب زیاد بنوش."
            else "Heat wave: today's high is ${Fmt.temp(today.tempMax, st)}; avoid midday outdoors and drink plenty of water.")
        if (today != null && today.tempMin <= th.tempLow) out += AlertItem(AlertType.COLD, "🥶",
            if (fa) "یخبندان: کمینهٔ امروز ${Fmt.temp(today.tempMin, st)}؛ لوله‌های آب روباز و مسیرهای لغزنده را جدی بگیر."
            else "Frost: today's low is ${Fmt.temp(today.tempMin, st)}; watch exposed pipes and slippery roads.")
        report.air?.dust?.let { dust ->
            if (dust >= 35) out += AlertItem(AlertType.DUST, "🏜️",
                if (fa) "گردوغبار در هواست؛ دید کم می‌شود و ماسک توصیه می‌شود."
                else "Dust in the air; visibility drops and a mask is recommended.")
        }
        report.aqiUs?.let { aqi ->
            when {
                aqi >= th.aqi -> out += AlertItem(AlertType.AQI, "😷",
                    if (fa) "کیفیت هوا ناسالمه (AQI ${Fmt.num(aqi, st.lang)})؛ ماسک بزن و فعالیت سنگین بیرون رو کم کن."
                    else "Air quality is unhealthy (AQI $aqi); wear a mask and cut heavy outdoor activity.")
                aqi > 100 -> out += AlertItem(AlertType.AQI, "🌫️",
                    if (fa) "کیفیت هوا برای گروه‌های حساس ناسالمه (AQI ${Fmt.num(aqi, st.lang)})."
                    else "Air quality is unhealthy for sensitive groups (AQI $aqi).")
            }
        }
        if (today != null && today.tempMax - today.tempMin >= 14) {
            out += AlertItem(AlertType.COLD, "🌡️",
                if (fa) "اختلاف دمای روز و شب زیاده؛ برای شب یه لایهٔ اضافه همراهت باشه."
                else "Big day–night temperature swing; carry an extra layer for the night.")
        }
        rainWindow(report, st)?.let { out += AlertItem(AlertType.RAIN, it.emoji, it.text) }
        return out
    }

    fun activities(report: WeatherReport, lang: Lang): List<ActivityScore> {
        val fa = lang == Lang.FA
        val c = report.current
        val rainProb = report.hourly.take(4).maxOfOrNull { it.precipProbability } ?: 0
        val kind = wmoInfo(c.weatherCode, c.isDay).kind
        val visibilityKm = (report.hourly.firstOrNull()?.visibilityM ?: 20_000.0) / 1000.0

        fun clamp(v: Int) = v.coerceIn(0, 100)
        fun dev(value: Double, ideal: Double, k: Int) = (kotlin.math.abs(value - ideal) * k).toInt()

        // NOTE: every penalty is its own Int. The previous `100 - if (a) x else 0 - if (b) y else 0`
        // form let each `else` swallow the rest of the expression, so e.g. strong wind or snow
        // could RAISE a score. Keep penalties as separate vals.
        val aqi = report.aqiUs ?: 0
        val storm = kind == SkyKind.STORM
        val fog = kind == SkyKind.FOG
        val snow = kind == SkyKind.SNOW

        val runScore = clamp(
            100 - dev(c.feelsLike, 14.0, 3) -
                (if (rainProb > 50) 45 else if (rainProb > 25) 20 else 0) -
                (if (c.windSpeed > 30) 20 else 0) -
                (if (aqi > 120) 50 else 0)
        )
        val walkScore = clamp(
            95 - dev(c.feelsLike, 20.0, 2) -
                (if (rainProb > 60) 50 else if (rainProb > 30) 20 else 0) -
                (if (storm) 60 else 0)
        )
        val photoScore = when (kind) {
            SkyKind.CLEAR_DAY -> 92
            SkyKind.CLEAR_NIGHT -> 88
            SkyKind.CLOUDY -> 68
            SkyKind.SNOW -> 80
            SkyKind.RAIN -> 45
            SkyKind.FOG -> 62
            SkyKind.STORM -> 30
        }
        val driveScore = clamp(
            100 -
                (if (fog) 55 else 0) -
                (if (snow) 45 else 0) -
                (if (rainProb > 70) 30 else if (rainProb > 40) 15 else 0) -
                (if (c.windSpeed > 45) 25 else 0) -
                (if (visibilityKm < 1) 40 else if (visibilityKm < 4) 15 else 0)
        )
        val cycleScore = clamp(
            95 - dev(c.feelsLike, 18.0, 2) -
                (if (rainProb > 40) 40 else if (rainProb > 20) 15 else 0) -
                (if (c.windSpeed > 25) 30 else if (c.windSpeed > 15) 10 else 0) -
                (if (aqi > 120) 45 else 0) -
                (if (storm) 70 else 0)
        )
        val hikeScore = clamp(
            95 - dev(c.feelsLike, 16.0, 2) -
                (if (rainProb > 50) 45 else if (rainProb > 25) 20 else 0) -
                (if (storm) 80 else 0) -
                (if (fog) 35 else 0) -
                (if (visibilityKm < 2) 25 else 0) -
                (if (c.windGusts > 50) 30 else 0)
        )
        fun note(score: Int, good: String, mid: String, bad: String) =
            if (score >= 70) good else if (score >= 40) mid else bad

        return if (fa) listOf(
            ActivityScore("دویدن", "🏃", runScore, note(runScore, "شرایط ایده‌آله", "قابل قبوله، سبک برو", "امروز توصیه نمی‌شه")),
            ActivityScore("پیاده‌روی", "🚶", walkScore, note(walkScore, "بهترین وقت پیاده‌رویه", "می‌شه رفت، آماده باش", "بهتره بمونی داخل")),
            ActivityScore("دوچرخه", "🚴", cycleScore, note(cycleScore, "مسیر برای رکاب‌زدن بازه", "باد رو در نظر بگیر", "امروز رکاب نزن")),
            ActivityScore("کوهنوردی", "⛰️", hikeScore, note(hikeScore, "شرایط کوه مناسبه", "با تجهیزات و احتیاط", "کوه امروز خطرناکه")),
            ActivityScore("عکاسی", "📸", photoScore, note(photoScore, "نور و آسمون عالین", "سوژه پیدا می‌شه", "نور امروز سخته")),
            ActivityScore("رانندگی", "🚗", driveScore, note(driveScore, "مسیر باز و امنه", "با احتیاط برو", "خیلی مراقب باش")),
        ) else listOf(
            ActivityScore("Running", "🏃", runScore, note(runScore, "Ideal conditions", "OK, take it easy", "Not recommended today")),
            ActivityScore("Walking", "🚶", walkScore, note(walkScore, "Best time for a walk", "Fine, be prepared", "Better stay in")),
            ActivityScore("Cycling", "🚴", cycleScore, note(cycleScore, "Roads are open for riding", "Mind the wind", "Skip riding today")),
            ActivityScore("Hiking", "⛰️", hikeScore, note(hikeScore, "Mountain conditions are good", "Gear up and be careful", "The mountain is risky today")),
            ActivityScore("Photography", "📸", photoScore, note(photoScore, "Light and sky are great", "Subjects to be found", "Tough light today")),
            ActivityScore("Driving", "🚗", driveScore, note(driveScore, "Roads open and safe", "Drive carefully", "Be extra careful")),
        )
    }

    /** 0..100 "how pleasant is it outside right now" — temperature feel, rain, wind and air quality. */
    fun comfortScore(report: WeatherReport): Int {
        val c = report.current
        val rainProb = report.hourly.take(4).maxOfOrNull { it.precipProbability } ?: 0
        val aqi = report.aqiUs ?: 0
        val kind = wmoInfo(c.weatherCode, c.isDay).kind
        val tempPenalty = (kotlin.math.abs(c.feelsLike - 21.0) * 3.0).toInt()
        val rainPenalty = if (rainProb > 70) 30 else if (rainProb > 40) 15 else if (rainProb > 20) 5 else 0
        val windPenalty = if (c.windSpeed > 50) 30 else if (c.windSpeed > 35) 18 else if (c.windSpeed > 20) 7 else 0
        val airPenalty = if (aqi > 200) 40 else if (aqi > 150) 28 else if (aqi > 100) 15 else if (aqi > 50) 4 else 0
        val skyPenalty = if (kind == SkyKind.STORM) 30 else if (kind == SkyKind.FOG) 10 else 0
        return (100 - tempPenalty - rainPenalty - windPenalty - airPenalty - skyPenalty).coerceIn(0, 100)
    }

    fun comfortLabel(score: Int, lang: Lang): String {
        val fa = lang == Lang.FA
        return when {
            score >= 85 -> if (fa) "عالی" else "Excellent"
            score >= 65 -> if (fa) "خوب" else "Good"
            score >= 40 -> if (fa) "متوسط" else "Fair"
            else -> if (fa) "ناخوشایند" else "Poor"
        }
    }

    fun aqiLabel(aqi: Int, lang: Lang): String {
        val fa = lang == Lang.FA
        return when {
            aqi <= 50 -> if (fa) "خوب" else "Good"
            aqi <= 100 -> if (fa) "قابل قبول" else "Moderate"
            aqi <= 150 -> if (fa) "ناسالم برای حساس‌ها" else "Unhealthy for sensitive groups"
            aqi <= 200 -> if (fa) "ناسالم" else "Unhealthy"
            aqi <= 300 -> if (fa) "بسیار ناسالم" else "Very unhealthy"
            else -> if (fa) "خطرناک" else "Hazardous"
        }
    }

    /** European AQI bands (0-20 good … >100 extremely poor) — different scale from US AQI. */
    fun aqiEuLabel(aqi: Int, lang: Lang): String {
        val fa = lang == Lang.FA
        return when {
            aqi <= 20 -> if (fa) "خوب" else "Good"
            aqi <= 40 -> if (fa) "نسبتاً خوب" else "Fair"
            aqi <= 60 -> if (fa) "متوسط" else "Moderate"
            aqi <= 80 -> if (fa) "ضعیف" else "Poor"
            aqi <= 100 -> if (fa) "خیلی ضعیف" else "Very poor"
            else -> if (fa) "به‌شدت ضعیف" else "Extremely poor"
        }
    }

    fun uvLabel(uv: Double, lang: Lang): String {
        val fa = lang == Lang.FA
        return when {
            uv < 3 -> if (fa) "کم" else "Low"
            uv < 6 -> if (fa) "متوسط" else "Moderate"
            uv < 8 -> if (fa) "زیاد" else "High"
            uv < 11 -> if (fa) "خیلی زیاد" else "Very high"
            else -> if (fa) "شدید" else "Extreme"
        }
    }

    fun windDirection(deg: Int, lang: Lang): String {
        val dirs = if (lang == Lang.FA)
            listOf("شمال", "شمال‌شرقی", "شرق", "جنوب‌شرقی", "جنوب", "جنوب‌غربی", "غرب", "شمال‌غربی")
        else
            listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return dirs[((deg + 22) / 45) % 8]
    }
}
