# Permission Scanner — مشخصات پروژه برای Agent

اپ اندرویدی **شخصی** (قرار نیست منتشر شود) که همهٔ اپ‌های نصب‌شده روی دستگاه را اسکن می‌کند، مجوزهای درخواستی (`requestedPermissions` در مانیفست) را استخراج و ریسک‌بندی می‌کند و به کاربر هشدار می‌دهد.

**اولویت: پیاده‌سازی درست، معماری تمیز، روان و سریع بودن.** دقت علمی دسته‌بندی مجوزها اولویت پایین‌تری دارد، ولی ساختار باید طوری باشد که تغییر آن آسان باشد.

---

## 1. Stack (اجباری)

| مورد | انتخاب |
|---|---|
| زبان | Kotlin (آخرین نسخهٔ پایدار) |
| UI | Jetpack Compose + Material 3 (`androidx.compose.material3`) + دوزبانه (انگلیسی پیش‌فرض + فارسی) |
| معماری | MVVM + Unidirectional Data Flow + Clean layering |
| Async | Kotlin Coroutines + Flow (`StateFlow`) |
| DI | Hilt |
| Navigation | Navigation Compose (route های type-safe با `@Serializable`) |
| Image | آیکون اپ‌ها از `PackageManager` با کش `LruCache` اختصاصی بر روی IO (بخش 6 را ببین) |
| minSdk / targetSdk | 24 / آخرین SDK پایدار |
| Build | Gradle Kotlin DSL + Version Catalog (`libs.versions.toml`) |
| Test | JUnit + MockK + Turbine (حداقل برای UseCase ها و ViewModel ها) |

بدون XML layout. بدون `LiveData`. بدون `findViewById`. بدون کتابخانهٔ اضافه‌ای که نیاز نیست.

---

## 2. ساختار پکیج‌ها

تک‌ماژول، ولی لایه‌ها با پکیج جدا شوند:

```
ir.arminniromandi.permissionchecker
├── App.kt                       // @HiltAndroidApp
├── MainActivity.kt              // setContent { AppTheme { AppNavHost() } }
├── di/                          // Hilt modules
├── domain/
│   ├── model/                   // AppInfo, PermissionInfo, RiskLevel, ScanProgress, ScanSummary
│   ├── repository/              // interface AppScanRepository
│   ├── risk/                    // RiskClassifier (منطق خالص، بدون Android)
│   └── usecase/                 // ScanAppsUseCase, GetAppDetailUseCase, ...
├── data/
│   ├── repository/              // AppScanRepositoryImpl
│   ├── source/                  // PackageScanner (تنها جایی که PackageManager لمس می‌شود)
│   └── mapper/
└── ui/
    ├── theme/                   // Color, Type, Theme (Material 3, dynamic color + fallback)
    ├── navigation/
    ├── common/                  // RiskBadge, AppIcon, EmptyState, ...
    ├── home/                    // HomeScreen, HomeViewModel, HomeUiState, HomeEvent
    ├── applist/                 // AppListScreen, AppListViewModel, ...
    └── detail/                  // DetailScreen, DetailViewModel, ...
```

### قوانین وابستگی
- `domain` به Android (به‌جز annotation های ساده) و به `data` و `ui` وابسته **نیست**.
- `ui` فقط به `domain` وابسته است؛ هرگز مستقیم به `PackageManager` دست نمی‌زند.
- `RiskClassifier` تابع خالص است و بدون `Context` تست‌پذیر.
- ViewModel ها `Context` یا `Activity` نگه نمی‌دارند.

---

## 3. مدل‌های دامنه

```kotlin
enum class RiskLevel { HIGH, MEDIUM, LOW, NONE }

data class PermissionInfo(
    val name: String,              // android.permission.READ_SMS
    val label: String?,            // برچسب خوانا از سیستم (nullable)
    val description: String?,
    val risk: RiskLevel,
    val isGranted: Boolean         // از requestedPermissionsFlags
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystemApp: Boolean,
    val permissions: List<PermissionInfo>,
    val overallRisk: RiskLevel
)

data class ScanProgress(
    val scanned: Int,
    val total: Int,
    val highCount: Int = 0,    // تعداد زندهٔ اپ‌های پرخطر تا این لحظه
    val mediumCount: Int = 0,  // تعداد زندهٔ اپ‌های مشکوک (متوسط) تا این لحظه
    val lowCount: Int = 0
)

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val progress: ScanProgress) : ScanState
    data class Done(val summary: ScanSummary) : ScanState
    data class Error(val message: String) : ScanState
}

data class ScanSummary(
    val userHigh: Int,
    val userMedium: Int,
    val userLow: Int,
    val userTotal: Int,
    val systemHigh: Int = 0,
    val systemMedium: Int = 0,
    val systemLow: Int = 0,
    val systemTotal: Int = 0
) {
    val totalApps: Int get() = userTotal + systemTotal
}
```

آیکون در مدل دامنه **نگهداری نمی‌شود** (Drawable سنگین است و به Android وابسته).

---

## 4. لایهٔ Data

### PackageScanner
- **اسکن دو مرحله‌ای (الزامی، برای progress واقعی و جلوگیری از `TransactionTooLargeException`):**
  1. مرحلهٔ اول: فقط لیست سبک پکیج‌ها بدون flag سنگین (`getInstalledPackages(0)` یا `getInstalledApplications(0)`) → `total` مشخص می‌شود. این مرحله سریع است؛ در این مدت UI حالت indeterminate / «در حال آماده‌سازی» نشان بدهد.
  2. مرحلهٔ دوم: برای هر پکیج جداگانه `getPackageInfo(pkg, GET_PERMISSIONS)` صدا زده شود و بعد از هر اپ `ScanProgress(scanned, total)` منتشر شود.
  - **ممنوع:** یک‌جا صدا زدن `getInstalledPackages(GET_PERMISSIONS)` برای همهٔ اپ‌ها؛ روی دستگاه‌های پر از اپ ممکن است crash کند و هیچ progress‌ای هم نمی‌دهد.
  - نسخهٔ API 33+ با `PackageInfoFlags.of(...)` و نسخهٔ deprecated برای پایین‌تر، هر دو پشت یک تابع wrapper.
  - اگر اسکن آنقدر سریع بود که progress در کمتر از ~300ms تمام شد، UI نباید فلش بزند: یک حداقل زمان نمایش (مثلاً 400ms) برای وضعیت Scanning در ViewModel اعمال شود.
- مجوزها: `PackageInfo.requestedPermissions` و `requestedPermissionsFlags` (بیت `REQUESTED_PERMISSION_GRANTED` برای وضعیت granted).
- سطح محافظت هر مجوز: `packageManager.getPermissionInfo(name, 0)` و خواندن protection level (`getProtection()` / `protectionLevel`). مجوزهای ناشناس (custom یا حذف‌شده) با `NameNotFoundException` → `try/catch` و مقدار پیش‌فرض.
- `QUERY_ALL_PACKAGES` باید در مانیفست اعلام شود (از Android 11 بدون آن لیست ناقص است). چون اپ شخصی است مشکل Play Policy نداریم.
- اپ‌های بدون مجوز (`requestedPermissions == null`) باید crash نکنند؛ لیست خالی.
- `isSystemApp`: `ApplicationInfo.FLAG_SYSTEM` ست باشد **و** `FLAG_UPDATED_SYSTEM_APP` نباشد (برای تشخیص حذف‌پذیر بودن، بخش 7).

### Repository
```kotlin
interface AppScanRepository {
    fun scan(): Flow<ScanEvent>              // Progress(scanned,total) ... Completed(list)
    val lastResult: StateFlow<List<AppInfo>> // کش در حافظه، singleton
    suspend fun getApp(packageName: String): AppInfo?
}
```
- Repository با `@Singleton` ارائه شود تا نتیجهٔ اسکن بین سه صفحه مشترک باشد (بدون پاس دادن لیست بزرگ در Navigation).
- **پردازش ترتیبی است (اولویت اصلی).** یک حلقهٔ ساده روی لیست پکیج‌ها، یک اپ در هر لحظه. **بدون** `async`، `chunked`، `awaitAll` یا هر نوع موازی‌سازی. با ترتیبی بودن، شمارنده‌ها و `scanned` نیاز به `AtomicInteger` یا قفل ندارند و باگ race وجود ندارد. موازی‌سازی فقط در صورت اثبات کندی با اندازه‌گیری واقعی و با تأیید صریح صاحب پروژه.
- اسکن با `flow { ... }.flowOn(ioDispatcher)` اجرا شود؛ هرگز روی Main. (دلیل `IO`: `getPackageInfo` یک فراخوانی Binder به system server است و blocking محسوب می‌شود.)
- لغو اسکن: `Flow` باید cancellation-aware باشد (`ensureActive()` در حلقه).

---

## 5. ریسک‌بندی (`RiskClassifier`)

- ورودی: نام مجوز + protection level. خروجی: `RiskLevel`.
- منطق در یک **map قابل ویرایش** (`Map<String, RiskLevel>`) در یک فایل جدا نگه‌داری شود، نه `when` های پراکنده.
- لیست اولیهٔ پیشنهادی (قابل تغییر، دقت کامل لازم نیست):
  - **HIGH**: `READ_SMS`, `RECEIVE_SMS`, `SEND_SMS`, `READ_CALL_LOG`, `RECORD_AUDIO`, `CAMERA`, `ACCESS_BACKGROUND_LOCATION`, `READ_CONTACTS`, `BIND_ACCESSIBILITY_SERVICE`, `SYSTEM_ALERT_WINDOW`, `REQUEST_INSTALL_PACKAGES`, `MANAGE_EXTERNAL_STORAGE`, `BIND_DEVICE_ADMIN`, `READ_PHONE_NUMBERS`
  - **MEDIUM**: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `READ_EXTERNAL_STORAGE`, `READ_MEDIA_*`, `READ_CALENDAR`, `BLUETOOTH_CONNECT`, `POST_NOTIFICATIONS`, `READ_PHONE_STATE`
  - **LOW**: بقیهٔ dangerous ها که بالا نیامده
  - **NONE**: normal permission ها
- Fallback: اگر مجوز در map نبود، از protection level: dangerous → MEDIUM، signature/privileged → LOW، normal → NONE.
- **منطق دقیق تعیین ریسک کل اپ (`overallRisk`):**
  - ضرایب امتیازی: `HIGH = 5`, `MEDIUM = 2`, `LOW = 1`, `NONE = 0`.
  - ثوابت نام‌دار آستانه: `HIGH_RISK_COUNT_THRESHOLD = 3` و `HIGH_RISK_SCORE_THRESHOLD = 12`.
  - **قانون محاسبه:**
    1. اگر تعداد مجوزهای HIGH اپ `highCount >= HIGH_RISK_COUNT_THRESHOLD` (۳ یا بیشتر) باشد یا مجموع کل امتیازات مجوزها `totalScore >= HIGH_RISK_SCORE_THRESHOLD` (۱۲ یا بیشتر) باشد → سطح ریسک کل اپ **`HIGH`** است.
    2. در غیر این صورت، اگر اپ حداقل یک مجوز `HIGH` یا حداقل یک مجوز `MEDIUM` داشته باشد (یک مجوز HIGH تکی به تنهایی اپ را HIGH نمی‌کند بلکه مشکوک/متوسط در نظر گرفته می‌شود) → سطح ریسک کل اپ **`MEDIUM`** است.
    3. در غیر این صورت، اگر اپ حداقل یک مجوز `LOW` داشته باشد → سطح ریسک کل اپ **`LOW`** است.
    4. در غیر این صورت (مجوزی درخواست نشده یا همه عادی/NONE هستند) → سطح ریسک کل اپ **`NONE`** است.
- **حتماً Unit Test** با چند اپ نمونه.

---

## 6. صفحات UI

وضعیت هر صفحه با یک `UiState` immutable (`data class`/`sealed interface`) در `StateFlow` ارائه شود؛ رویدادهای کاربر به‌صورت تابع روی ViewModel یا `sealed interface Event`. رویدادهای یک‌باره (مثل باز کردن intent حذف) از `Channel`/`SharedFlow` بیایند، نه از state. Composable ها با `collectAsStateWithLifecycle()` مصرف کنند و **stateless** باشند (state hoisting).

### 6.1 صفحهٔ اصلی (Home)
- یک دکمهٔ بزرگ مرکزی «شروع اسکن».
- هنگام اسکن: دکمه غیرفعال یا تبدیل به «لغو»، `LinearProgressIndicator` (determinate) و متن «۳ از ۲۰ برنامه اسکن شد».
- **شمارندهٔ زنده در حین اسکن:** کنار progress دو عدد زنده نشان داده شود: «پرخطر: N» و «مشکوک: M». قانون:
  - بعد از بررسی **کامل مجوزهای یک اپ** و محاسبهٔ `overallRisk` همان اپ، اگر `HIGH` بود `highCount` یک واحد زیاد شود؛ اگر `MEDIUM` بود `mediumCount`؛ اگر `LOW`/`NONE` بود `lowCount`.
  - شمارش **به ازای هر اپ یک‌بار** است، نه به ازای هر مجوز (وگرنه یک اپ با ۵ مجوز خطرناک ۵ بار شمرده می‌شود).
  - شمارنده‌ها و `scanned` در **یک** `ScanProgress` و با هم منتشر شوند تا UI هیچ‌وقت وضعیت ناسازگار نبیند (مثلاً scanned=5 ولی مجموع شمارنده‌ها=6).
  - چون پردازش ترتیبی است، شمارنده‌ها متغیرهای محلی ساده داخل حلقهٔ `flow` هستند؛ نیازی به `AtomicInteger` نیست.
  - عدد نهایی شمارنده‌ها باید دقیقاً با `ScanSummary` یکی باشد (تست واحد بنویس).
- بعد از اتمام: کارت خلاصه — تمرکز اصلی کارت خلاصه روی اپ‌های نصب‌شدهٔ کاربر (User Apps) است (تعداد اپ‌های پرخطر، مشکوک/متوسط، کم‌خطر و مجموع کاربر)، و آمار اپ‌های سیستمی به صورت تفکیک‌شده و ثانویه در پایین کارت یا در بخشی مجزا ذکر می‌شود تا نویز اپ‌های پیش‌فرض سیستم تجربه کاربر را مخدوش نکند. یک دکمه «مشاهده لیست برنامه‌ها».
- اگر نتیجه‌ای از قبل در حافظه است (مثلاً بعد از بازگشت از حذف)، خلاصه نمایش داده شود.
- حالت Error با پیام و دکمهٔ تلاش مجدد.
- اگر پرخطر وجود داشت، یک **هشدار** واضح (Card با رنگ `errorContainer`) نمایش داده شود.

### 6.2 لیست اپ‌ها
- `LazyColumn` با `key = packageName` (برای عملکرد و حفظ state).
- هر آیتم: آیکون اپ، نام اپ، نام پکیج، نشانگر ریسک (آیکون/نقطه رنگی با رنگ معنادار + متن/آیکون جدا تا فقط به رنگ متکی نباشد).
- `SearchBar`/`TextField` جستجو بر اساس نام (و اختیاری پکیج)، با `debounce` ~200ms در ViewModel.
- `FilterChip`: همه / پرخطر / متوسط / کم‌خطر. همچنین یک چیپ برای شامل کردن یا نشامل کردن اپ‌های سیستمی (پیش‌فرض: شامل نشود، ولی در اسکن همه بررسی می‌شوند).
- مرتب‌سازی پیش‌فرض: ریسک نزولی، سپس نام.
- فیلتر و جستجو در ViewModel با `combine(...)` روی Flow ها، **نه** در Composable.
- Empty state برای نبود نتیجه.

### 6.3 جزئیات
- بالا: آیکون بزرگ + نام اپ.
- کارت اطلاعات: نام پکیج، `versionName` (و `versionCode`)، تاریخ نصب، تاریخ آخرین آپدیت (فرمت محلی).
- بخش اصلی: لیست کامل مجوزهای درخواستی؛ هر ردیف: نام خوانا (در صورت وجود) + نام کامل مجوز، وضعیت (داده شده/نشده) و نشانگر ریسک رنگی. گروه‌بندی یا مرتب‌سازی بر اساس ریسک.
- پایین (ثابت): دکمهٔ **Uninstall** (بخش 7).
- مجوزهای خالی: پیام «این برنامه مجوزی درخواست نکرده».

### آیکون اپ‌ها (عملکرد)
- `packageManager.getApplicationIcon(pkg)` سنگین و blocking است؛ **در Composable صدا زده نشود**.
- **راه‌حل انتخاب‌شده:** کش در حافظه اختصاصی `LruCache<String, ImageBitmap>` به همراه همزمانی امن Coroutines بر روی `Dispatchers.IO` و Deduplication درخواست‌های در جریان (`ConcurrentHashMap<String, Deferred<ImageBitmap?>>`). این روش بالاترین نرخ فریم (بدون jank)، مصرف بهینه رم و صفر وابستگی اضافی به کتابخانه‌های سنگین را تضمین می‌کند.
- Drawable به `ImageBitmap` با اندازهٔ ثابت بهینه (مثلاً 96dp) مقیاس‌بندی و تبدیل شود تا حافظه کنترل شود.
- Placeholder تا لود شدن.

---

## 7. حذف برنامه

```kotlin
val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
```
- با `ActivityResultContracts.StartActivityForResult` اجرا شود.
- در ViewModel فقط یک `Event.RequestUninstall(packageName)` منتشر شود؛ خود intent در لایهٔ UI ساخته و launch می‌شود.
- بعد از بازگشت: بررسی کن پکیج هنوز نصب است یا نه. اگر حذف شد → از کش حذف و به لیست برگرد (`popBackStack`) و خلاصهٔ Home آپدیت شود؛ اگر کنسل شد → کاری نکن.
- **اپ‌های سیستمی** (حذف‌ناپذیر): دکمه غیرفعال یا متن توضیحی نشان بده، نه اینکه intent بی‌اثر باز شود. حذف اپ‌های سیستمی از طریق intent عادی ممکن نیست.
- `REQUEST_DELETE_PACKAGES` برای `ACTION_DELETE` لازم نیست؛ فقط اگر از `ACTION_UNINSTALL_PACKAGE` استفاده شد اعلام شود.
- قبل از ارسال intent، یک `AlertDialog` تأیید نشان بده.

---

## 8. Navigation

سه route: `Home` → `AppList` → `Detail(packageName)`.
- فقط `packageName` (String) پاس داده شود؛ `DetailViewModel` اپ را از Repository (کش) می‌گیرد (`SavedStateHandle`).
- اگر اپ در کش نبود (مثلاً process death) → state خطا یا بازگشت به Home.
- state اسکن باید از چرخش صفحه و process recreation جان سالم به در ببرد (حداقل چرخش).

---

## 9. Material Design

- Material 3، `dynamicColorScheme` در API 31+ با fallback به یک `lightColorScheme/darkColorScheme` ثابت.
- پشتیبانی Dark mode.
- رنگ‌های ریسک به‌صورت semantic در Theme تعریف شوند (مثلاً `RiskColors`: high=قرمز، medium=نارنجی/زرد، low=سبز/آبی) با نسخهٔ روشن و تیره، نه hardcode در Composable ها.
- Edge-to-edge، `Scaffold`، `TopAppBar`، `Card`, `FilterChip`, `LinearProgressIndicator`.
- انیمیشن‌های سبک (`AnimatedVisibility`, `animateFloatAsState` برای progress)؛ زیاده‌روی نکن.
- طراحی «خوب و تمیز» کافی است، پیچیدگی بصری لازم نیست.

---

## 10. عملکرد و کیفیت

- هیچ کار سنگینی روی Main thread.
- Composable ها پایدار (stable) باشند: مدل‌ها immutable، لیست‌ها با `ImmutableList` یا `@Immutable`/`@Stable` در صورت نیاز.
- `remember`/`derivedStateOf` فقط جایی که لازم است.
- بدون memory leak: ViewModel ها `Context` نگه ندارند؛ `Application` فقط از طریق Hilt و فقط در data layer.
- Error handling: `runCatching` در مرز data، تبدیل به state خطا در UI؛ هیچ exception بدون مدیریت.
- Lint و `ktlint`/`detekt` (اختیاری) بدون warning مهم.
- Logging با `Timber` یا `Log` فقط در debug.

---

## 10.5. Coroutines و Dispatchers (قوانین سخت‌گیرانه)

### Dispatcher ها
- Dispatcher ها **هرگز hardcode نشوند** (`Dispatchers.IO` مستقیم داخل کلاس‌ها ممنوع). با Hilt و `@Qualifier` تزریق شوند:
  ```kotlin
  @Qualifier annotation class IoDispatcher
  @Qualifier annotation class DefaultDispatcher
  @Qualifier annotation class MainDispatcher
  ```
  Module ای که `Dispatchers.IO/Default/Main` را provide می‌کند در `di/`. در تست‌ها با `StandardTestDispatcher` جایگزین می‌شوند.
- تخصیص:
  - **IO**: همهٔ تماس‌های `PackageManager` (اسکن مجوزها، `getPermissionInfo`)، لود و کش آیکون.
  - **Default**: کارهای CPU-bound (در این اپ تقریباً چیزی نیست؛ `RiskClassifier` سبک است و جدا کردن آن به Default لازم نیست).
  - **Main**: فقط UI. ViewModel ها با `viewModelScope` (Main.immediate) کار می‌کنند و خودشان هیچ کار سنگینی نمی‌کنند.
- **Main-safety:** هر تابع `suspend` در Repository/UseCase باید خودش main-safe باشد (اگر نیاز بود داخلش `withContext(io)`)، تا فراخواننده نیازی به دانستن Dispatcher نداشته باشد.
- ViewModel و Composable مستقیم `withContext` نمی‌زنند.

### Flow
- **داخل `flow { }` هرگز با `withContext` تغییر context نده** و `emit` را داخل `withContext` صدا نزن (نقض Flow invariant و exception). برای تغییر Dispatcher فقط از `flowOn` استفاده کن.
- `flowOn` باید **آخر** زنجیرهٔ upstream باشد و فقط upstream را تحت تأثیر می‌گذارد.
- progress را با `conflate()` یا `StateFlow` مصرف کن؛ UI فقط آخرین مقدار را لازم دارد، و اسکن به‌خاطر UI کند نمی‌شود.
- state ها با `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)` یا `MutableStateFlow` + `update { }` (atomic) تغییر کنند.

### Scope و Lifecycle
- فقط `viewModelScope` (یا scope تزریق‌شدهٔ `@ApplicationScope` برای چیزهای عمدتاً singleton). **`GlobalScope` ممنوع.**
- `Job` اسکن در ViewModel نگه‌داری شود؛ شروع مجدد وقتی اسکن در جریان است نادیده گرفته شود (جلوگیری از اسکن دوبل با دو بار کلیک)، و لغو با `job.cancel()` کار کند.
- UI فقط با `collectAsStateWithLifecycle()` جمع کند.

### Cancellation و خطا
- `CancellationException` **هرگز بلعیده نشود.** در `try/catch` یا `runCatching` حتماً rethrow شود (`catch (e: CancellationException) { throw e }`). `runCatching` روی کد suspend خطرناک است؛ یا رفتار آن را مدیریت کن یا `try/catch` صریح بنویس.
- در حلقهٔ اسکن، قبل از هر اپ `currentCoroutineContext().ensureActive()`.
- خطای یک اپ (مثلاً `NameNotFoundException` چون اپ هم‌زمان حذف شد) **کل اسکن را نمی‌شکند**؛ همان اپ رد شود و اسکن ادامه یابد.
- پاک‌سازی (مثلاً ریست state) در `finally` یا `NonCancellable` فقط جایی که واقعاً لازم است.

### تست
- `runTest` + `StandardTestDispatcher` تزریق‌شده؛ با Turbine ترتیب emission ها (`Scanning(0/N) → ... → Done`) و لغو اسکن تست شود. تست `delay` ها (حداقل زمان نمایش) با زمان مجازی.

---

## 11. مانیفست

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```
هیچ مجوز دیگری (اینترنت و...) لازم نیست. **اپ نباید هیچ داده‌ای را خارج از دستگاه بفرستد.**

---

## 12. Definition of Done

- [ ] اسکن اپ‌های کاربر و سیستمی، با progress دقیق «X از Y»
- [ ] خلاصهٔ نتایج بعد از اسکن
- [ ] لیست با جستجو و فیلتر ریسک، روان (بدون jank) با ۱۰۰+ اپ
- [ ] صفحهٔ جزئیات کامل با لیست مجوزها و رنگ ریسک
- [ ] دکمهٔ حذف با intent، refresh بعد از بازگشت، مدیریت اپ سیستمی
- [ ] MVVM تمیز، بدون وابستگی معکوس بین لایه‌ها
- [ ] تست واحد برای `RiskClassifier`، `ScanAppsUseCase` و ViewModel ها
- [ ] build بدون error و بدون crash روی API 24 و آخرین API

## 13. ترتیب پیشنهادی پیاده‌سازی

1. Gradle/Version Catalog، Hilt، Theme، Navigation خالی
2. Domain models + `RiskClassifier` + تست
3. `PackageScanner` + Repository + `ScanAppsUseCase`
4. Home (اسکن + progress + خلاصه)
5. لیست اپ‌ها (آیکون، جستجو، فیلتر)
6. جزئیات + حذف
7. پولیش، تست‌ها، بررسی عملکرد
