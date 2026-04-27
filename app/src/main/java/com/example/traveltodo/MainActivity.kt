package com.example.traveltodo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.*

val DarkBg = Color(0xFF0A0A0A)
val DarkCard = Color(0xFF1A1A1A)
val DarkCard2 = Color(0xFF242424)
val AccentBlue = Color(0xFF4A9FFF)
val AccentGreen = Color(0xFF00E676)
val AccentOrange = Color(0xFFFF6B35)
val AccentPurple = Color(0xFFBB86FC)
val AccentPink = Color(0xFFFF4081)
val AccentYellow = Color(0xFFFFD600)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAAAAAA)
val TextHint = Color(0xFF666666)

data class User(val name: String, val email: String, val phone: String, val password: String)
data class Place(val id: String, val name: String, val category: String, val address: String, val city: String, val rating: Float, val reviewCount: Int, val priceRange: String, val distance: String, val hours: String, val phone: String, val description: String, val emoji: String, val lat: Double = 0.0, val lon: Double = 0.0, val imageUrl: String = "")
data class Booking(val id: String, val placeName: String, val date: String, val time: String, val people: Int, val amount: Double, var status: String = "Active")
data class Ticket(val id: String, val placeName: String, val eventDate: String, val qty: Int, val amount: Double, var status: String = "Active")
data class Review(val id: String, val placeName: String, val userName: String, var rating: Int, var comment: String, val date: String, var isOwn: Boolean = false)
data class Payment(val id: String, val amount: Double, val method: String, val date: String, val status: String, val txnId: String)

fun isWithin2Hours(date: String, time: String): Boolean {
    return try {
        val parts = date.split("-")
        val timeParts = time.split(":")
        if (parts.size != 3) return false
        val cal = Calendar.getInstance()
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), timeParts.getOrNull(0)?.toIntOrNull() ?: 0, timeParts.getOrNull(1)?.toIntOrNull() ?: 0, 0)
        val diff = cal.timeInMillis - System.currentTimeMillis()
        diff in 0..(2 * 60 * 60 * 1000L)
    } catch (e: Exception) { false }
}

fun isWithin48Hours(date: String): Boolean {
    return try {
        val parts = date.split("-")
        if (parts.size != 3) return false
        val cal = Calendar.getInstance()
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
        val diff = cal.timeInMillis - System.currentTimeMillis()
        diff in 0..(48 * 60 * 60 * 1000L)
    } catch (e: Exception) { false }
}

object AppData {
    val users = mutableListOf(
        User("Ashish Mishra", "test@test.com", "817-555-0101", "1234"),
        User("Demo User", "demo@demo.com", "817-555-0102", "demo123")
    )
    var currentUser: User? = null
    var loginAttempts = 0
    var currentBookingPlace: Place? = null
    var userLat: Double = 32.814
    var userLon: Double = -97.0641
    val nearbyPlaces = mutableStateListOf<Place>()

    val defaultPlaces = listOf(
        Place("r1", "Pizza Palace", "Restaurants", "3880 W Airport Fwy", "Irving, TX", 4.5f, 234, "\$\$", "0.3 mi", "10AM-10PM", "817-555-1001", "Best pizza in Irving with wood-fired ovens.", "🍕", 32.814, -97.0641, "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400"),
        Place("r2", "Burger Barn", "Restaurants", "2250 E Grauwyler Rd", "Irving, TX", 4.2f, 189, "\$", "0.8 mi", "9AM-11PM", "817-555-1002", "Classic American burgers with hand-cut fries.", "🍔", 32.8237, -97.0109, "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400"),
        Place("r3", "Sushi Garden", "Restaurants", "5600 N MacArthur Blvd", "Irving, TX", 4.7f, 312, "\$\$\$", "1.2 mi", "11AM-10PM", "817-555-1003", "Authentic Japanese cuisine with fresh sushi.", "🍣", 32.8573, -97.0109, "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=400"),
        Place("r4", "Taco Fiesta", "Restaurants", "1900 Story Rd", "Irving, TX", 4.3f, 156, "\$", "0.5 mi", "8AM-12AM", "817-555-1004", "Authentic Mexican street tacos.", "🌮", 32.8168, -97.0109, "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=400"),
        Place("r5", "The Steakhouse", "Restaurants", "4200 W John Carpenter Fwy", "Irving, TX", 4.8f, 445, "\$\$\$\$", "2.1 mi", "5PM-11PM", "817-555-1005", "Premium steaks aged 28 days.", "🥩", 32.8771, -97.0109, "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=400"),
        Place("a1", "Game Zone", "Arcade", "3880 Irving Mall", "Irving, TX", 4.4f, 567, "\$\$", "1.0 mi", "10AM-10PM", "817-555-2001", "200+ arcade games, laser tag.", "🎮", 32.8168, -97.0641, "https://images.unsplash.com/photo-1511882150382-421056c89033?w=400"),
        Place("a2", "VR World", "Arcade", "7800 N MacArthur Blvd", "Irving, TX", 4.6f, 189, "\$\$\$", "2.3 mi", "12PM-10PM", "817-555-2003", "Cutting-edge VR experiences.", "🥽", 32.8771, -97.0641, "https://images.unsplash.com/photo-1622979135225-d2ba269cf1ac?w=400"),
        Place("s1", "Sky Adventures", "Skydiving", "4201 Conflans Rd", "Irving, TX", 4.9f, 123, "\$\$\$\$", "5.2 mi", "8AM-6PM", "817-555-3001", "Tandem skydiving with certified instructors.", "🪂", 32.8168, -97.0109, "https://images.unsplash.com/photo-1601024445121-e5b82f020549?w=400"),
        Place("s2", "Drop Zone DFW", "Skydiving", "1600 Aviation Dr", "Irving, TX", 4.7f, 98, "\$\$\$\$", "6.1 mi", "7AM-7PM", "817-555-3002", "Premier skydiving facility.", "✈️", 32.8237, -97.0109, "https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=400"),
        Place("ap1", "Wonder Park", "Amusement Park", "2600 E Grauwyler Rd", "Irving, TX", 4.5f, 789, "\$\$\$", "3.4 mi", "9AM-9PM", "817-555-4001", "Family amusement park with 25+ rides.", "🎡", 32.8237, -97.0109, "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400"),
        Place("b1", "Strike Lanes", "Bowling", "3400 W Pioneer Dr", "Irving, TX", 4.4f, 345, "\$\$", "1.8 mi", "10AM-12AM", "817-555-5001", "24-lane bowling with arcade.", "🎳", 32.8168, -97.0641, "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400"),
        Place("b2", "Lucky Strikes", "Bowling", "6700 N MacArthur Blvd", "Irving, TX", 4.6f, 456, "\$\$\$", "3.1 mi", "12PM-2AM", "817-555-5003", "Upscale bowling lounge.", "🎯", 32.8573, -97.0641, "https://images.unsplash.com/photo-1520095972714-909e91b038e5?w=400")
    )

    val bookings = mutableStateListOf<Booking>()
    val tickets = mutableStateListOf<Ticket>()
    val reviews = mutableStateListOf(
        Review("rv1", "Pizza Palace", "John D.", 5, "Amazing pizza! Best in Irving.", "2026-04-10"),
        Review("rv2", "Pizza Palace", "Sarah M.", 4, "Great food, worth it.", "2026-04-12"),
        Review("rv3", "Game Zone", "Mike R.", 5, "Kids loved the laser tag!", "2026-04-11"),
        Review("rv4", "Sky Adventures", "Lisa K.", 5, "Life changing experience!", "2026-04-09")
    )
    val payments = mutableStateListOf<Payment>()
    val searchPlaceCache = mutableMapOf<String, Place>()
}

object PlacesAPI {
    private val client = OkHttpClient()

    suspend fun searchPlaces(query: String): List<Place> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = Uri.encode(query)
            val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=20&addressdetails=1"
            val request = Request.Builder().url(url).header("User-Agent", "TravelToDo/1.0").build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONArray(body)
            val results = mutableListOf<Place>()
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                val displayName = item.getString("display_name")
                val nameParts = displayName.split(",")
                val name = nameParts.getOrNull(0)?.trim() ?: displayName
                val city = nameParts.getOrNull(2)?.trim() ?: ""
                val address = nameParts.take(3).joinToString(", ")
                val lat = item.getDouble("lat")
                val lon = item.getDouble("lon")
                val osmClass = item.optString("class", "")
                val type = item.optString("type", "place")
                if (osmClass == "boundary" || (osmClass == "place" && (type == "city" || type == "county" || type == "state" || type == "country"))) continue
                val category = getCategoryFromType(type, name)
                val rating = (3.5f + (Math.random() * 1.5f)).toFloat()
                val placeId = "osm$i"
                val imageUrl = getImageForCategory(category)
                val place = Place(id = placeId, name = name, category = category, address = address, city = city, rating = Math.round(rating * 10) / 10f, reviewCount = (50 + (Math.random() * 500).toInt()), priceRange = listOf("\$", "\$\$", "\$\$\$").random(), distance = "${"%.1f".format(Math.random() * 10)} mi", hours = "9AM-10PM", phone = "N/A", description = "Located at $address. A popular destination.", emoji = getEmojiForCategory(category), lat = lat, lon = lon, imageUrl = imageUrl)
                AppData.searchPlaceCache[placeId] = place
                results.add(place)
            }
            results
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getNearbyPlaces(lat: Double, lon: Double): List<Place> = withContext(Dispatchers.IO) {
        try {
            val radius = 5000
            val query = "[out:json][timeout:25];(node[\"amenity\"=\"restaurant\"](around:$radius,$lat,$lon);node[\"amenity\"=\"fast_food\"](around:$radius,$lat,$lon);node[\"amenity\"=\"cafe\"](around:$radius,$lat,$lon);node[\"leisure\"=\"bowling_alley\"](around:$radius,$lat,$lon);node[\"tourism\"=\"attraction\"](around:$radius,$lat,$lon);node[\"leisure\"=\"park\"](around:$radius,$lat,$lon););out body;"
            val encodedQuery = Uri.encode(query)
            val url = "https://overpass-api.de/api/interpreter?data=$encodedQuery"
            val request = Request.Builder().url(url).header("User-Agent", "TravelToDo/1.0").build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = org.json.JSONObject(body)
            val elements = json.getJSONArray("elements")
            val results = mutableListOf<Place>()
            for (i in 0 until minOf(elements.length(), 20)) {
                val item = elements.getJSONObject(i)
                val tags = item.optJSONObject("tags") ?: continue
                val name = tags.optString("name", "").ifEmpty { continue }
                val placeLat = item.optDouble("lat", 0.0)
                val placeLon = item.optDouble("lon", 0.0)
                val street = tags.optString("addr:street", "")
                val housenumber = tags.optString("addr:housenumber", "")
                val city = tags.optString("addr:city", "")
                val address = if (housenumber.isNotEmpty() && street.isNotEmpty()) "$housenumber $street" else if (street.isNotEmpty()) street else "Near your location"
                val amenity = tags.optString("amenity", tags.optString("leisure", tags.optString("tourism", "")))
                val category = when (amenity) {
                    "restaurant", "fast_food", "cafe" -> "Restaurants"
                    "bowling_alley" -> "Bowling"
                    "arcade", "amusement_arcade" -> "Arcade"
                    "attraction" -> "Entertainment"
                    "park" -> "Amusement Park"
                    else -> "Entertainment"
                }
                val distKm = calculateDistance(lat, lon, placeLat, placeLon)
                val distStr = if (distKm < 1.0) "${"%.0f".format(distKm * 1000)} m" else "${"%.1f".format(distKm)} km"
                val rating = (3.5f + (Math.random() * 1.5f)).toFloat()
                val placeId = "ovp$i"
                val place = Place(id = placeId, name = name, category = category, address = address, city = city.ifEmpty { "Near You" }, rating = Math.round(rating * 10) / 10f, reviewCount = (50 + (Math.random() * 500).toInt()), priceRange = listOf("\$", "\$\$", "\$\$\$").random(), distance = distStr, hours = tags.optString("opening_hours", "9AM-10PM"), phone = tags.optString("phone", "N/A"), description = "A real place near your location.", emoji = getEmojiForCategory(category), lat = placeLat, lon = placeLon, imageUrl = getImageForCategory(category))
                AppData.searchPlaceCache[placeId] = place
                results.add(place)
            }
            results.sortedBy { it.distance }
        } catch (e: Exception) { emptyList() }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
    }

    private fun getCategoryFromType(type: String, name: String): String {
        val n = name.lowercase()
        return when {
            n.contains("restaurant") || n.contains("pizza") || n.contains("burger") || n.contains("cafe") || n.contains("food") -> "Restaurants"
            n.contains("arcade") || n.contains("game") || n.contains("vr") -> "Arcade"
            n.contains("skydiv") || n.contains("jump") -> "Skydiving"
            n.contains("park") || n.contains("amusement") -> "Amusement Park"
            n.contains("bowl") || n.contains("lanes") -> "Bowling"
            else -> "Entertainment"
        }
    }

    private fun getEmojiForCategory(category: String): String = when (category) {
        "Restaurants" -> listOf("🍕", "🍔", "🍣", "🌮", "🍜").random()
        "Arcade" -> "🎮"
        "Skydiving" -> "🪂"
        "Amusement Park" -> "🎡"
        "Bowling" -> "🎳"
        else -> listOf("📍", "🏛️", "🎭", "🏪", "🎪").random()
    }

    private fun getImageForCategory(category: String): String = when (category) {
        "Restaurants" -> listOf("https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400", "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400", "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=400").random()
        "Arcade" -> "https://images.unsplash.com/photo-1511882150382-421056c89033?w=400"
        "Skydiving" -> "https://images.unsplash.com/photo-1601024445121-e5b82f020549?w=400"
        "Amusement Park" -> "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400"
        "Bowling" -> "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400"
        else -> "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=400"
    }
}

class MainActivity : ComponentActivity() {
    private val locationPermissionRequest = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationPermissionRequest.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = AccentBlue, background = DarkBg, surface = DarkCard, onBackground = TextPrimary, onSurface = TextPrimary)) {
                TravelApp()
            }
        }
    }
}

@Composable
fun TravelApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute in listOf("home", "bookings", "tickets", "profile")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = DarkCard, contentColor = TextPrimary) {
                    NavigationBarItem(selected = currentRoute == "home", onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home", fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentBlue, selectedTextColor = AccentBlue, unselectedIconColor = TextHint, unselectedTextColor = TextHint, indicatorColor = AccentBlue.copy(alpha = 0.15f)))
                    NavigationBarItem(selected = currentRoute == "bookings", onClick = { navController.navigate("bookings") }, icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("Bookings", fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentBlue, selectedTextColor = AccentBlue, unselectedIconColor = TextHint, unselectedTextColor = TextHint, indicatorColor = AccentBlue.copy(alpha = 0.15f)))
                    NavigationBarItem(selected = currentRoute == "tickets", onClick = { navController.navigate("tickets") }, icon = { Icon(Icons.Default.Star, null) }, label = { Text("Tickets", fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentOrange, selectedTextColor = AccentOrange, unselectedIconColor = TextHint, unselectedTextColor = TextHint, indicatorColor = AccentOrange.copy(alpha = 0.15f)))
                    NavigationBarItem(selected = currentRoute == "profile", onClick = { navController.navigate("profile") }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile", fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGreen, selectedTextColor = AccentGreen, unselectedIconColor = TextHint, unselectedTextColor = TextHint, indicatorColor = AccentGreen.copy(alpha = 0.15f)))
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = navController, startDestination = "splash") {
                composable("splash") { SplashScreen(navController) }
                composable("login") { LoginScreen(navController) }
                composable("register") { RegisterScreen(navController) }
                composable("forgot") { ForgotPasswordScreen(navController) }
                composable("home") { HomeScreen(navController) }
                composable("places/{category}") { back -> PlacesScreen(navController, back.arguments?.getString("category") ?: "") }
                composable("detail/{placeId}") { back -> DetailScreen(navController, back.arguments?.getString("placeId") ?: "") }
                composable("bookings") { BookingsScreen(navController) }
                composable("tickets") { TicketsScreen(navController) }
                composable("profile") { ProfileScreen(navController) }
                composable("book") { BookingFormScreen(navController) }
                composable("buyticket") { TicketFormScreen(navController) }
                composable("payment/{type}/{extra}") { back -> PaymentScreen(navController, back.arguments?.getString("type") ?: "", back.arguments?.getString("extra") ?: "") }
                composable("reviews/{placeId}") { back -> ReviewScreen(navController, back.arguments?.getString("placeId") ?: "") }
            }
        }
    }
}

@Composable
fun SplashScreen(nav: NavController) {
    LaunchedEffect(Unit) {
        delay(2500)
        nav.navigate("login") { popUpTo("splash") { inclusive = true } }
    }
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), DarkBg, DarkBg))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(36.dp)).background(Brush.linearGradient(listOf(AccentBlue, AccentPurple))), contentAlignment = Alignment.Center) {
                Text("✈️", fontSize = 64.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text("Travel ToDo", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Discover. Book. Explore.", fontSize = 16.sp, color = TextSecondary)
            Spacer(Modifier.height(48.dp))
            CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
        }
    }
}

@Composable
fun LoginScreen(nav: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), DarkBg, DarkBg)))) {
        Column(modifier = Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(AccentBlue, AccentPurple))), contentAlignment = Alignment.Center) { Text("✈️", fontSize = 44.sp) }
                Spacer(Modifier.height(20.dp))
                Text("Travel ToDo", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text("Discover. Book. Explore.", fontSize = 15.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(48.dp))
            val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = TextHint, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = AccentBlue, unfocusedContainerColor = DarkCard, focusedContainerColor = DarkCard)
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.Email, null, tint = AccentBlue) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = fieldColors)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.Lock, null, tint = AccentBlue) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = fieldColors)
            if (error.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(error, color = AccentPink, fontSize = 13.sp) }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { nav.navigate("forgot") }) { Text("Forgot Password?", color = AccentBlue) }
            Spacer(Modifier.height(16.dp))
            GradientButton("Login", listOf(AccentBlue, AccentPurple)) {
                when {
                    email.isEmpty() || password.isEmpty() -> error = "Please fill all fields"
                    AppData.loginAttempts >= 3 -> error = "Account locked. Use Forgot Password."
                    AppData.users.any { it.email == email && it.password == password } -> {
                        AppData.currentUser = AppData.users.first { it.email == email }
                        AppData.loginAttempts = 0
                        nav.navigate("home") { popUpTo("login") { inclusive = true } }
                    }
                    else -> { AppData.loginAttempts++; val r = 3 - AppData.loginAttempts; error = if (r > 0) "Invalid credentials. $r attempts left." else "Account locked. Use Forgot Password." }
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { nav.navigate("register") }, modifier = Modifier.fillMaxWidth()) {
                Text("Don't have an account? ", color = TextSecondary)
                Text("Register", color = AccentBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RegisterScreen(nav: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = TextHint, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = AccentBlue, unfocusedContainerColor = DarkCard, focusedContainerColor = DarkCard)

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
            item {
                Text("Create Account", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text("Join Travel ToDo today", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(32.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.Person, null, tint = AccentBlue) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = fieldColors)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.Email, null, tint = AccentBlue) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = fieldColors)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.Phone, null, tint = AccentBlue) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = fieldColors)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password (min 6 chars)", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.Lock, null, tint = AccentBlue) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = fieldColors)
                if (message.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(message, color = if (isSuccess) AccentGreen else AccentPink, fontSize = 13.sp) }
                Spacer(Modifier.height(24.dp))
                GradientButton("Create Account", listOf(AccentBlue, AccentPurple)) {
                    when {
                        name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() -> { message = "All fields are required"; isSuccess = false }
                        !email.contains("@") -> { message = "Invalid email format"; isSuccess = false }
                        AppData.users.any { it.email == email } -> { message = "❌ Email already exists!"; isSuccess = false }
                        password.length < 6 -> { message = "Password must be at least 6 characters"; isSuccess = false }
                        else -> { AppData.users.add(User(name, email, phone, password)); message = "✅ Account created! Please login."; isSuccess = true }
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { nav.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Already have an account? ", color = TextSecondary)
                    Text("Login", color = AccentBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(nav: NavController) {
    var email by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = TextHint, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = AccentBlue, unfocusedContainerColor = DarkCard, focusedContainerColor = DarkCard)

    Box(modifier = Modifier.fillMaxSize().background(DarkBg).padding(28.dp), contentAlignment = Alignment.Center) {
        Column {
            Text("Forgot Password", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text("Reset your password", fontSize = 14.sp, color = TextSecondary)
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Registered Email", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.Email, null, tint = AccentBlue) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = fieldColors)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Security: What city are you from?", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.Info, null, tint = AccentBlue) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = fieldColors)
            if (message.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(message, color = if (isSuccess) AccentGreen else AccentPink, fontSize = 13.sp) }
            Spacer(Modifier.height(24.dp))
            GradientButton("Reset Password", if (attempts < 3) listOf(AccentBlue, AccentPurple) else listOf(TextHint, TextHint), enabled = attempts < 3) {
                when {
                    attempts >= 3 -> message = "❌ Locked after 3 failed attempts."
                    !AppData.users.any { it.email == email } -> { attempts++; message = "❌ Email not found. ${3 - attempts} attempts left."; isSuccess = false }
                    !answer.equals("Irving", ignoreCase = true) -> { attempts++; message = "❌ Wrong answer. ${3 - attempts} attempts left."; isSuccess = false }
                    else -> { message = "✅ Temp password: Temp@1234"; isSuccess = true }
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { nav.popBackStack() }, modifier = Modifier.fillMaxWidth()) { Text("← Back to Login", color = AccentBlue) }
        }
    }
}

@Composable
fun GradientButton(text: String, colors: List<Color>, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(colors)).clickable(enabled = enabled) { onClick() }, contentAlignment = Alignment.Center) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (enabled) TextPrimary else TextHint)
    }
}

@Composable
fun HomeScreen(nav: NavController) {
    val categories = listOf("Restaurants", "Arcade", "Skydiving", "Amusement Park", "Bowling")
    var search by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Place>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchDone by remember { mutableStateOf(false) }
    var locationLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        locationLoading = true
        try {
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        AppData.userLat = location.latitude
                        AppData.userLon = location.longitude
                        scope.launch {
                            val nearby = PlacesAPI.getNearbyPlaces(location.latitude, location.longitude)
                            AppData.nearbyPlaces.clear()
                            AppData.nearbyPlaces.addAll(nearby)
                            locationLoading = false
                        }
                    } else { locationLoading = false }
                }
            } else { locationLoading = false }
        } catch (e: Exception) { locationLoading = false }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), DarkBg))).padding(24.dp)) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Hello, ${AppData.currentUser?.name?.split(" ")?.first() ?: "Traveler"}! 👋", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                    Text(" Find activities anywhere", fontSize = 14.sp, color = AccentBlue)
                                }
                            }
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AccentBlue, AccentPurple))).clickable { nav.navigate("profile") }, contentAlignment = Alignment.Center) {
                                Text(AppData.currentUser?.name?.first()?.toString() ?: "U", fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontSize = 20.sp)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it; if (it.isEmpty()) { searchResults = emptyList(); searchDone = false } },
                            placeholder = { Text("Search places, activities...", color = TextHint, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = AccentBlue) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = Color.Transparent, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = AccentBlue, unfocusedContainerColor = DarkCard2, focusedContainerColor = DarkCard2),
                            trailingIcon = {
                                if (isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AccentBlue, strokeWidth = 2.dp)
                                else if (search.isNotEmpty()) IconButton(onClick = {
                                    isSearching = true; searchDone = false
                                    scope.launch { searchResults = PlacesAPI.searchPlaces(search); isSearching = false; searchDone = true }
                                }) { Icon(Icons.Default.Search, null, tint = AccentBlue) }
                            }
                        )
                    }
                }
            }

            if (search.isNotEmpty() && (isSearching || searchDone)) {
                item {
                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentBlue)
                                Spacer(Modifier.height(12.dp))
                                Text("Searching...", color = TextSecondary)
                            }
                        }
                    } else {
                        Text(if (searchResults.isEmpty()) "No results found" else "${searchResults.size} places found", modifier = Modifier.padding(16.dp), color = TextSecondary, fontSize = 14.sp)
                    }
                }
                items(searchResults) { place -> DarkPlaceCard(place, nav) }
            } else {
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        DarkQuickAction("Bookings", Icons.Default.DateRange, AccentBlue) { nav.navigate("bookings") }
                        DarkQuickAction("Tickets", Icons.Default.Star, AccentOrange) { nav.navigate("tickets") }
                        DarkQuickAction("Profile", Icons.Default.Person, AccentGreen) { nav.navigate("profile") }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Categories", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(16.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(categories) { category -> DarkCategoryChip(category) { nav.navigate("places/$category") } }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Featured Places", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(16.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(AppData.defaultPlaces.sortedByDescending { it.rating }.take(5)) { place ->
                            FeaturedCard(place) { AppData.currentBookingPlace = place; nav.navigate("detail/${place.id}") }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("📍 Real Places Near You", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(16.dp))
                }
                if (locationLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentBlue)
                                Spacer(Modifier.height(12.dp))
                                Text("Finding real places near you...", color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(if (AppData.nearbyPlaces.isNotEmpty()) AppData.nearbyPlaces else AppData.defaultPlaces) { place -> DarkPlaceCard(place, nav) }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun DarkQuickAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(color.copy(alpha = 0.15f)).border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DarkCategoryChip(category: String, onClick: () -> Unit) {
    val (emoji, gradient) = when (category) {
        "Restaurants" -> Pair("🍕", listOf(Color(0xFFFF6B35), Color(0xFFFF8C00)))
        "Arcade" -> Pair("🎮", listOf(AccentBlue, AccentPurple))
        "Skydiving" -> Pair("🪂", listOf(Color(0xFF00B4D8), Color(0xFF0077B6)))
        "Amusement Park" -> Pair("🎡", listOf(AccentGreen, Color(0xFF00BFA5)))
        "Bowling" -> Pair("🎳", listOf(AccentYellow, Color(0xFFFF8F00)))
        else -> Pair("📍", listOf(TextHint, TextHint))
    }
    Box(modifier = Modifier.width(120.dp).height(100.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(gradient)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 32.sp)
            Spacer(Modifier.height(6.dp))
            Text(category, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun FeaturedCard(place: Place, onClick: () -> Unit) {
    Box(modifier = Modifier.width(200.dp).height(240.dp).clip(RoundedCornerShape(20.dp)).clickable { onClick() }) {
        AsyncImage(model = place.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(place.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("📍 ${place.city}", fontSize = 12.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⭐ ${place.rating}", fontSize = 13.sp, color = AccentYellow, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AccentBlue.copy(alpha = 0.8f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(place.priceRange, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DarkPlaceCard(place: Place, nav: NavController) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp).clickable { AppData.currentBookingPlace = place; nav.navigate("detail/${place.id}") }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = DarkCard), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp))) {
                AsyncImage(model = place.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                Text(place.emoji, fontSize = 28.sp, modifier = Modifier.align(Alignment.Center))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(place.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("📍 ${place.address}", fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐ ${place.rating}", fontSize = 13.sp, color = AccentYellow, fontWeight = FontWeight.Bold)
                    Text(" (${place.reviewCount})", fontSize = 11.sp, color = TextHint)
                    Spacer(Modifier.width(8.dp))
                    Text(place.priceRange, fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(place.distance, fontSize = 12.sp, color = TextHint)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallBtn("Book", AccentBlue) { AppData.currentBookingPlace = place; nav.navigate("book") }
                    SmallBtn("Ticket", AccentOrange) { AppData.currentBookingPlace = place; nav.navigate("buyticket") }
                    SmallBtn("Map", AccentGreen) {
                        val uri = Uri.parse("geo:${place.lat},${place.lon}?q=${Uri.encode(place.address)}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                }
            }
        }
    }
}

@Composable
fun SmallBtn(text: String, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.2f)).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PlacesScreen(nav: NavController, category: String) {
    val places = AppData.defaultPlaces.filter { it.category == category }
    val gradient = when (category) {
        "Restaurants" -> listOf(Color(0xFFFF6B35), Color(0xFFFF8C00))
        "Arcade" -> listOf(AccentBlue, AccentPurple)
        "Skydiving" -> listOf(Color(0xFF00B4D8), Color(0xFF0077B6))
        "Amusement Park" -> listOf(AccentGreen, Color(0xFF00BFA5))
        "Bowling" -> listOf(AccentYellow, Color(0xFFFF8F00))
        else -> listOf(AccentBlue, AccentPurple)
    }
    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(gradient)).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                    Text(category, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("(${places.size} places)", fontSize = 14.sp, color = TextPrimary.copy(alpha = 0.7f))
                }
            }
            if (places.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😕", fontSize = 64.sp); Spacer(Modifier.height(16.dp))
                        Text("No places available", fontSize = 20.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text("Check back later!", fontSize = 14.sp, color = TextHint)
                    }
                }
            } else {
                LazyColumn {
                    items(places) { place -> DarkPlaceCard(place, nav) }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(nav: NavController, placeId: String) {
    val context = LocalContext.current
    val place = AppData.defaultPlaces.find { it.id == placeId } ?: AppData.currentBookingPlace ?: return
    val placeReviews = AppData.reviews.filter { it.placeName == place.name }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        LazyColumn {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    AsyncImage(model = place.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.8f)))))
                    IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.padding(16.dp)) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                        Text(place.emoji + " " + place.name, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("📍 ${place.address}, ${place.city}", fontSize = 14.sp, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐ ${place.rating}", fontSize = 16.sp, color = AccentYellow, fontWeight = FontWeight.Bold)
                            Text(" (${place.reviewCount} reviews)", fontSize = 13.sp, color = TextSecondary)
                            Spacer(Modifier.width(12.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AccentBlue.copy(alpha = 0.8f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(place.priceRange, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("About", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(12.dp))
                        DarkInfoRow(Icons.Default.DateRange, "Hours: ${place.hours}")
                        Spacer(Modifier.height(8.dp))
                        DarkInfoRow(Icons.Default.Phone, place.phone)
                        Spacer(Modifier.height(8.dp))
                        DarkInfoRow(Icons.Default.Star, "${place.priceRange} • ${place.distance} away")
                        Spacer(Modifier.height(12.dp))
                        Text(place.description, fontSize = 14.sp, color = TextSecondary, lineHeight = 22.sp)
                    }
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DarkActionBtn("📅 Book Activity", Brush.linearGradient(listOf(AccentBlue, AccentPurple)), Modifier.weight(1f)) { AppData.currentBookingPlace = place; nav.navigate("book") }
                        DarkActionBtn("🎟️ Buy Ticket", Brush.linearGradient(listOf(AccentOrange, Color(0xFFFF8C00))), Modifier.weight(1f)) { AppData.currentBookingPlace = place; nav.navigate("buyticket") }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DarkActionBtn("⭐ Review", Brush.linearGradient(listOf(AccentYellow, Color(0xFFFF8F00))), Modifier.weight(1f)) { nav.navigate("reviews/${place.id}") }
                        DarkActionBtn("🗺️ Directions", Brush.linearGradient(listOf(AccentGreen, Color(0xFF00BFA5))), Modifier.weight(1f)) {
                            val uri = Uri.parse("geo:${place.lat},${place.lon}?q=${Uri.encode(place.address + ", " + place.city)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            if (placeReviews.isNotEmpty()) {
                item { Text("Reviews (${placeReviews.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(horizontal = 16.dp)); Spacer(Modifier.height(8.dp)) }
                items(placeReviews) { review ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(review.userName, fontWeight = FontWeight.Bold, color = TextPrimary); Text(review.date, fontSize = 12.sp, color = TextHint) }
                            Text("${"⭐".repeat(review.rating)} ${review.rating}/5", color = AccentYellow)
                            if (review.comment.isNotEmpty()) Text(review.comment, fontSize = 14.sp, color = TextSecondary)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun DarkActionBtn(text: String, brush: Brush, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.height(52.dp).clip(RoundedCornerShape(14.dp)).background(brush).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
fun DarkInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
fun ReviewScreen(nav: NavController, placeId: String) {
    val place = AppData.defaultPlaces.find { it.id == placeId } ?: AppData.currentBookingPlace ?: return
    var reviewText by remember { mutableStateOf("") }
    var reviewRating by remember { mutableStateOf(5) }
    var message by remember { mutableStateOf("") }
    var editingReview by remember { mutableStateOf<Review?>(null) }
    val placeReviews = remember { AppData.reviews.filter { it.placeName == place.name }.toMutableStateList() }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(AccentYellow, Color(0xFFFF8F00)))).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                    Text("Reviews - ${place.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(if (editingReview != null) "Edit Review" else "Write a Review", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(Modifier.height(12.dp))
                            Text("Rating: ${"⭐".repeat(reviewRating)} ($reviewRating/5)", color = AccentYellow, fontWeight = FontWeight.Medium)
                            Slider(value = reviewRating.toFloat(), onValueChange = { reviewRating = it.toInt() }, valueRange = 1f..5f, steps = 3, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = AccentYellow, activeTrackColor = AccentYellow))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = reviewText, onValueChange = { reviewText = it }, label = { Text("Share your experience...", color = TextSecondary) }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp), maxLines = 5, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentYellow, unfocusedBorderColor = TextHint, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, unfocusedContainerColor = DarkCard2, focusedContainerColor = DarkCard2))
                            if (message.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(message, color = AccentGreen) }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(AccentYellow, Color(0xFFFF8F00)))).clickable {
                                    if (editingReview != null) {
                                        val index = placeReviews.indexOf(editingReview)
                                        if (index >= 0) placeReviews[index] = editingReview!!.copy(rating = reviewRating, comment = reviewText)
                                        val globalIndex = AppData.reviews.indexOfFirst { it.id == editingReview!!.id }
                                        if (globalIndex >= 0) AppData.reviews[globalIndex] = AppData.reviews[globalIndex].copy(rating = reviewRating, comment = reviewText)
                                        message = "✅ Review updated!"; editingReview = null; reviewText = ""; reviewRating = 5
                                    } else {
                                        val review = Review("rv${System.currentTimeMillis()}", place.name, AppData.currentUser?.name ?: "User", reviewRating, reviewText, "2026-04-20", isOwn = true)
                                        AppData.reviews.add(review); placeReviews.add(review); message = "✅ Review submitted!"; reviewText = ""
                                    }
                                }, contentAlignment = Alignment.Center) { Text(if (editingReview != null) "Update Review" else "Submit Review", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
                                if (editingReview != null) {
                                    Box(modifier = Modifier.height(52.dp).clip(RoundedCornerShape(14.dp)).background(TextHint.copy(alpha = 0.2f)).clickable { editingReview = null; reviewText = ""; reviewRating = 5 }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("All Reviews (${placeReviews.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                }
                items(placeReviews) { review ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(review.userName, fontWeight = FontWeight.Bold, color = TextPrimary); Text(review.date, fontSize = 12.sp, color = TextHint) }
                            Text("${"⭐".repeat(review.rating)} ${review.rating}/5", color = AccentYellow)
                            if (review.comment.isNotEmpty()) Text(review.comment, fontSize = 14.sp, color = TextSecondary)
                            if (review.isOwn) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { editingReview = review; reviewText = review.comment; reviewRating = review.rating }) { Text("✏️ Edit", color = AccentBlue) }
                                    TextButton(onClick = { AppData.reviews.remove(review); placeReviews.remove(review) }) { Text("🗑️ Delete", color = AccentPink) }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun BookingFormScreen(nav: NavController) {
    val place = AppData.currentBookingPlace ?: return
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var people by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = TextHint, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = AccentBlue, unfocusedContainerColor = DarkCard2, focusedContainerColor = DarkCard2)

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(AccentBlue, AccentPurple))).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                    Text("Book Activity", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))) {
                                AsyncImage(model = place.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Text(place.emoji, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column { Text(place.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary); Text("📍 ${place.address}", fontSize = 13.sp, color = TextSecondary); Text("⭐ ${place.rating} • ${place.priceRange}", fontSize = 13.sp, color = AccentYellow) }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Booking Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (e.g. 2026-05-01)", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.DateRange, null, tint = AccentBlue) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (e.g. 14:00)", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.DateRange, null, tint = AccentBlue) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = people, onValueChange = { people = it }, label = { Text("Number of People", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.Person, null, tint = AccentBlue) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                    val isDuplicate = AppData.bookings.any { it.placeName == place.name && it.date == date && it.time == time && it.status == "Active" }
                    if (isDuplicate) { Spacer(Modifier.height(8.dp)); Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = AccentPink.copy(alpha = 0.1f))) { Text("⚠️ You already have a booking for this place at this time!", modifier = Modifier.padding(12.dp), color = AccentPink, fontSize = 13.sp) } }
                    if (error.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(error, color = AccentPink) }
                    Spacer(Modifier.height(16.dp))
                    if (people.isNotEmpty()) {
                        val p = people.toIntOrNull() ?: 0
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.15f))) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Estimated Total:", color = TextSecondary)
                                Text("$${"%.2f".format(p * 25.0)}", fontWeight = FontWeight.ExtraBold, color = AccentBlue, fontSize = 22.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    GradientButton("Proceed to Payment →", listOf(AccentBlue, AccentPurple)) {
                        when {
                            date.isEmpty() || time.isEmpty() || people.isEmpty() -> error = "Please fill all fields"
                            people.toIntOrNull() == null || (people.toIntOrNull() ?: 0) <= 0 -> error = "Enter valid number of people"
                            isDuplicate -> error = "You already have a booking at this time!"
                            else -> nav.navigate("payment/booking/${date}_${time}_${people}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketFormScreen(nav: NavController) {
    val place = AppData.currentBookingPlace ?: return
    var date by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange, unfocusedBorderColor = TextHint, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = AccentOrange, unfocusedContainerColor = DarkCard2, focusedContainerColor = DarkCard2)

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(AccentOrange, Color(0xFFFF8C00)))).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                    Text("Buy Ticket", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))) {
                                AsyncImage(model = place.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Text(place.emoji, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column { Text(place.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary); Text("📍 ${place.address}", fontSize = 13.sp, color = TextSecondary); Text("⭐ ${place.rating} • ${place.priceRange}", fontSize = 13.sp, color = AccentYellow) }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Ticket Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Event Date (e.g. 2026-05-01)", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.DateRange, null, tint = AccentOrange) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Number of Tickets", color = TextSecondary) }, leadingIcon = { Icon(Icons.Default.Star, null, tint = AccentOrange) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = fieldColors)
                    if (error.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(error, color = AccentPink) }
                    Spacer(Modifier.height(16.dp))
                    if (qty.isNotEmpty()) {
                        val q = qty.toIntOrNull() ?: 0
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.15f))) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Estimated Total:", color = TextSecondary)
                                Text("$${"%.2f".format(q * 15.0)}", fontWeight = FontWeight.ExtraBold, color = AccentOrange, fontSize = 22.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    GradientButton("Proceed to Payment →", listOf(AccentOrange, Color(0xFFFF8C00))) {
                        when {
                            date.isEmpty() || qty.isEmpty() -> error = "Please fill all fields"
                            qty.toIntOrNull() == null || (qty.toIntOrNull() ?: 0) <= 0 -> error = "Enter valid number of tickets"
                            else -> nav.navigate("payment/ticket/${date}_${qty}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentScreen(nav: NavController, type: String, extra: String) {
    val place = AppData.currentBookingPlace ?: AppData.defaultPlaces.first()
    val parts = extra.split("_")
    var paymentMethod by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf(false) }
    var txnId by remember { mutableStateOf("") }
    val amount = if (type == "booking") (parts.getOrNull(2)?.toIntOrNull() ?: 1) * 25.0 else (parts.getOrNull(1)?.toIntOrNull() ?: 1) * 15.0

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!paid) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                    Text("Payment", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    if (paid) {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AccentGreen, Color(0xFF00BFA5)))), contentAlignment = Alignment.Center) { Text("✅", fontSize = 40.sp) }
                                Spacer(Modifier.height(16.dp))
                                Text("Payment Successful!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AccentGreen)
                                Spacer(Modifier.height(8.dp))
                                Text("Transaction ID: $txnId", fontSize = 13.sp, color = TextHint)
                                Spacer(Modifier.height(24.dp))
                                HorizontalDivider(color = TextHint.copy(alpha = 0.3f))
                                Spacer(Modifier.height(16.dp))
                                ReceiptRow("Place", place.name)
                                if (type == "booking") { ReceiptRow("Date", parts.getOrNull(0) ?: ""); ReceiptRow("Time", parts.getOrNull(1) ?: ""); ReceiptRow("People", parts.getOrNull(2) ?: "") }
                                else { ReceiptRow("Event Date", parts.getOrNull(0) ?: ""); ReceiptRow("Tickets", parts.getOrNull(1) ?: "") }
                                ReceiptRow("Method", paymentMethod)
                                HorizontalDivider(color = TextHint.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Paid:", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                                    Text("$${"%.2f".format(amount)}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = AccentGreen)
                                }
                                Spacer(Modifier.height(24.dp))
                                GradientButton("View My ${if (type == "booking") "Bookings" else "Tickets"}", listOf(AccentGreen, Color(0xFF00BFA5))) { nav.navigate(if (type == "booking") "bookings" else "tickets") { popUpTo("home") } }
                                Spacer(Modifier.height(12.dp))
                                TextButton(onClick = { nav.navigate("home") { popUpTo("home") { inclusive = true } } }, modifier = Modifier.fillMaxWidth()) { Text("Back to Home", color = TextSecondary) }
                            }
                        }
                    } else {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Order Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(Modifier.height(16.dp))
                                ReceiptRow("Place", place.name)
                                if (type == "booking") { ReceiptRow("Date", parts.getOrNull(0) ?: ""); ReceiptRow("Time", parts.getOrNull(1) ?: ""); ReceiptRow("People", "${parts.getOrNull(2)} × $25") }
                                else { ReceiptRow("Event Date", parts.getOrNull(0) ?: ""); ReceiptRow("Tickets", "${parts.getOrNull(1)} × $15") }
                                HorizontalDivider(color = TextHint.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total:", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                                    Text("$${"%.2f".format(amount)}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = AccentBlue)
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("Select Payment Method", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(12.dp))
                        listOf(Triple("💳 Credit Card", "Visa / Mastercard", AccentBlue), Triple("📱 Digital Wallet", "PayPal / Apple Pay", AccentPurple), Triple("💵 Cash", "Pay at venue", AccentGreen)).forEach { (method, sub, color) ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { paymentMethod = method }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (paymentMethod == method) color.copy(alpha = 0.15f) else DarkCard), border = if (paymentMethod == method) BorderStroke(2.dp, color) else BorderStroke(1.dp, TextHint.copy(alpha = 0.2f))) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = paymentMethod == method, onClick = { paymentMethod = method }, colors = RadioButtonDefaults.colors(selectedColor = color))
                                    Spacer(Modifier.width(12.dp))
                                    Column { Text(method, fontWeight = FontWeight.Bold, color = if (paymentMethod == method) color else TextPrimary); Text(sub, fontSize = 12.sp, color = TextHint) }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        GradientButton("Confirm & Pay $${"%.2f".format(amount)}", if (paymentMethod.isNotEmpty()) listOf(AccentBlue, AccentPurple) else listOf(TextHint, TextHint), enabled = paymentMethod.isNotEmpty()) {
                            txnId = "TXN${System.currentTimeMillis().toString().takeLast(6)}"
                            AppData.payments.add(Payment(txnId, amount, paymentMethod, "2026-04-20", "Success", txnId))
                            if (type == "booking") AppData.bookings.add(Booking("B${System.currentTimeMillis().toString().takeLast(4)}", place.name, parts.getOrNull(0) ?: "", parts.getOrNull(1) ?: "", parts.getOrNull(2)?.toIntOrNull() ?: 1, amount))
                            else AppData.tickets.add(Ticket("T${System.currentTimeMillis().toString().takeLast(4)}", place.name, parts.getOrNull(0) ?: "", parts.getOrNull(1)?.toIntOrNull() ?: 1, amount))
                            paid = true
                        }
                        if (paymentMethod.isEmpty()) { Spacer(Modifier.height(8.dp)); Text("⚠️ Please select a payment method", color = AccentPink, fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BookingsScreen(nav: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(AccentBlue, AccentPurple))).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                    Text("My Bookings (${AppData.bookings.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
            if (AppData.bookings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📅", fontSize = 64.sp); Spacer(Modifier.height(16.dp))
                        Text("No bookings yet", fontSize = 20.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text("Book an activity to get started!", fontSize = 14.sp, color = TextHint)
                    }
                }
            } else {
                LazyColumn {
                    items(AppData.bookings) { booking ->
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(booking.placeName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (booking.status == "Active") AccentGreen.copy(alpha = 0.2f) else AccentPink.copy(alpha = 0.2f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text(booking.status, color = if (booking.status == "Active") AccentGreen else AccentPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("📅 ${booking.date} at ${booking.time}", fontSize = 14.sp, color = TextSecondary)
                                Text("👥 ${booking.people} people", fontSize = 14.sp, color = TextSecondary)
                                Text("💰 $${"%.2f".format(booking.amount)}", fontSize = 16.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
                                Text("🎫 Booking ID: ${booking.id}", fontSize = 12.sp, color = TextHint)
                                if (booking.status == "Active") {
                                    Spacer(Modifier.height(12.dp))
                                    if (isWithin2Hours(booking.date, booking.time)) {
                                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = TextHint.copy(alpha = 0.1f))) {
                                            Text("⚠️ Cannot cancel — booking is within 2 hours", modifier = Modifier.padding(12.dp), color = TextHint, fontSize = 13.sp, textAlign = TextAlign.Center)
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(AccentPink.copy(alpha = 0.15f)).border(1.dp, AccentPink.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).clickable {
                                            val index = AppData.bookings.indexOf(booking)
                                            if (index >= 0) AppData.bookings[index] = booking.copy(status = "Cancelled")
                                        }, contentAlignment = Alignment.Center) { Text("Cancel Booking", color = AccentPink, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun TicketsScreen(nav: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(AccentOrange, Color(0xFFFF8C00)))).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                    Text("My Tickets (${AppData.tickets.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
            if (AppData.tickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎟️", fontSize = 64.sp); Spacer(Modifier.height(16.dp))
                        Text("No tickets yet", fontSize = 20.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text("Buy tickets to your favorite events!", fontSize = 14.sp, color = TextHint)
                    }
                }
            } else {
                LazyColumn {
                    items(AppData.tickets) { ticket ->
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(ticket.placeName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (ticket.status == "Active") AccentGreen.copy(alpha = 0.2f) else AccentPink.copy(alpha = 0.2f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text(ticket.status, color = if (ticket.status == "Active") AccentGreen else AccentPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("📅 Event: ${ticket.eventDate}", fontSize = 14.sp, color = TextSecondary)
                                Text("🎟️ Quantity: ${ticket.qty} tickets", fontSize = 14.sp, color = TextSecondary)
                                Text("💰 $${"%.2f".format(ticket.amount)}", fontSize = 16.sp, color = AccentOrange, fontWeight = FontWeight.Bold)
                                Text("🎫 Ticket ID: ${ticket.id}", fontSize = 12.sp, color = TextHint)
                                if (ticket.status == "Active") {
                                    Spacer(Modifier.height(12.dp))
                                    if (isWithin48Hours(ticket.eventDate)) {
                                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = TextHint.copy(alpha = 0.1f))) {
                                            Text("⚠️ Cannot cancel — event is within 48 hours. Non-refundable.", modifier = Modifier.padding(12.dp), color = TextHint, fontSize = 13.sp, textAlign = TextAlign.Center)
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(AccentPink.copy(alpha = 0.15f)).border(1.dp, AccentPink.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).clickable {
                                            val index = AppData.tickets.indexOf(ticket)
                                            if (index >= 0) AppData.tickets[index] = ticket.copy(status = "Cancelled")
                                        }, contentAlignment = Alignment.Center) { Text("Cancel Ticket", color = AccentPink, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(nav: NavController) {
    val user = AppData.currentUser
    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        LazyColumn {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), DarkBg))), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) } }
                        Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AccentBlue, AccentPurple))), contentAlignment = Alignment.Center) {
                            Text(user?.name?.first()?.toString() ?: "U", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(user?.name ?: "User", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text(user?.email ?: "", fontSize = 14.sp, color = TextSecondary)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DarkStatBox("Bookings", AppData.bookings.size.toString(), AccentBlue)
                    DarkStatBox("Tickets", AppData.tickets.size.toString(), AccentOrange)
                    DarkStatBox("Reviews", AppData.reviews.count { it.isOwn }.toString(), AccentGreen)
                }
                Spacer(Modifier.height(24.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Account Info", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(16.dp))
                        DarkInfoRow(Icons.Default.Person, user?.name ?: "")
                        Spacer(Modifier.height(12.dp))
                        DarkInfoRow(Icons.Default.Email, user?.email ?: "")
                        Spacer(Modifier.height(12.dp))
                        DarkInfoRow(Icons.Default.Phone, user?.phone ?: "")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp).clip(RoundedCornerShape(16.dp)).background(AccentPink.copy(alpha = 0.15f)).border(1.dp, AccentPink.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).clickable {
                    AppData.currentUser = null; AppData.loginAttempts = 0
                    nav.navigate("login") { popUpTo("home") { inclusive = true } }
                }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ExitToApp, null, tint = AccentPink, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentPink)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DarkStatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(18.dp)).background(color.copy(alpha = 0.15f)).border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}