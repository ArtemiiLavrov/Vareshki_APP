package com.example.vareshki

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mysql.jdbc.Connection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId

class LoginViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> get() = _products

    private val _unitsOfMeasurement = MutableStateFlow<List<UnitOfMeasurement>>(emptyList())
    val unitsOfMeasurement: StateFlow<List<UnitOfMeasurement>> get() = _unitsOfMeasurement

    private val _orderViewStatus = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val orderViewStatus: StateFlow<Map<Int, Boolean>> = _orderViewStatus.asStateFlow()

    private val _userRole = MutableStateFlow<Int?>(null)
    val userRole: StateFlow<Int?> = _userRole.asStateFlow()

    private val _canteens = MutableStateFlow<List<Canteen>>(emptyList())
    val canteens: StateFlow<List<Canteen>> get() = _canteens.asStateFlow()
    var canteenId = mutableStateOf<Int?>(null)
    var canteenAddress = mutableStateOf<String?>(null)

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> get() = _employees.asStateFlow()

    private val _statuses = MutableStateFlow<List<OrderStatus>>(emptyList())
    val statuses: StateFlow<List<OrderStatus>> get() = _statuses.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Кэш статусов и столовых
    private val _statusCache = MutableStateFlow<Map<Int, String>>(emptyMap())
    val statusCache: StateFlow<Map<Int, String>> = _statusCache.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _canteenCache = MutableStateFlow<Map<Int, String>>(emptyMap())
    val canteenCache: StateFlow<Map<Int, String>> = _canteenCache.asStateFlow()

    // Кэш для истории изменений по orderId
    private val _historyCache = mutableMapOf<Int, MutableStateFlow<List<StatusChange>>>()

    private val connectionString = "jdbc:mysql://server76.hosting.reg.ru:3306/u2902799_Vareshki" +
            "?user=u2902799_ONYXAdm&password=Onyx159357&useSSL=false&allowPublicKeyRetrieval=true" +
            "&characterEncoding=utf8&useUnicode=true&connectionCollation=utf8_unicode_ci"

    private val prefs = context.getSharedPreferences("VareshkiPrefs", Context.MODE_PRIVATE)

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
        ToastUtils.showToast(context, message)
    }

    suspend fun loadStatuses(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val statuses = fetchStatuses()
            _statusCache.value = statuses.associate { it.statusId to it.statusName }
            Result.success(Unit)
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка загрузки статусов: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun getEmployeeCanteenID(): Int = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        var resultSet: java.sql.ResultSet? = null
        var preparedStatement: java.sql.PreparedStatement? = null
        try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = """
            SELECT canteen
            FROM employees
            WHERE employeeID = ?
        """.trimIndent()

            preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, getEmployeeId())

            resultSet = preparedStatement.executeQuery()
            if (resultSet.next()) resultSet.getInt("canteen") else -1
        } catch (e: Exception) {
            println("Failed to fetch employee canteen: ${e.message}")
            -1
        } finally {
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
        }
    }

    suspend fun fetchActualQuantitiesWithAcceptance(orderId: Int): Map<Int, ProductStatus> = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        var resultSet: java.sql.ResultSet? = null
        var preparedStatement: java.sql.PreparedStatement? = null
        val result = mutableMapOf<Int, ProductStatus>()
        try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            // Получаем canteenExecutorID для заказа
            val executorQuery = """
            SELECT canteenCustomerID
            FROM orders
            WHERE orderID = ?
        """.trimIndent()
            preparedStatement = connection.prepareStatement(executorQuery)
            preparedStatement.setInt(1, orderId)
            resultSet = preparedStatement.executeQuery()
            val canteenExecutorID = if (resultSet.next()) resultSet.getInt("canteenCustomerID") else -1
            resultSet.close()
            preparedStatement.close()

            // Проверяем, принадлежит ли сотрудник столовой-исполнителю
            val employeeCanteenID = getEmployeeCanteenID()
            if (canteenExecutorID == -1 || employeeCanteenID != canteenExecutorID) {
                return@withContext emptyMap()
            }

            // Загружаем все записи для данного orderID
            val query = """
            SELECT productID, actualQuantity, isAccepted
            FROM actualSentQuantities
            WHERE orderID = ?
        """.trimIndent()
            preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, orderId)

            resultSet = preparedStatement.executeQuery()
            while (resultSet.next()) {
                val productId = resultSet.getInt("productID")
                val quantity = resultSet.getDouble("actualQuantity")
                val isAccepted = resultSet.getBoolean("isAccepted")
                result[productId] = ProductStatus(productId, quantity, isAccepted)
            }
        } catch (e: Exception) {
            println("Failed to fetch actual quantities: ${e.message}")
        } finally {
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
        }
        result
    }

    // Новый метод для обновления статуса принятия
    suspend fun updateAcceptanceStatus(
        orderId: Int,
        productId: Int,
        isAccepted: Boolean?,
        employeeId: Int
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Updating acceptance status: orderId=$orderId, productId=$productId, isAccepted=$isAccepted, employeeId=$employeeId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = """
                INSERT INTO actualSentQuantities (orderID, productID, actualQuantity, sentDate, sentByEmployeeID, isAccepted)
                VALUES (?, ?, 0.0, NOW(), ?, ?)
                ON DUPLICATE KEY UPDATE 
                    isAccepted = ?,
                    sentDate = NOW(),
                    sentByEmployeeID = ?
            """.trimIndent()

            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, orderId)
            preparedStatement.setInt(2, productId)
            preparedStatement.setInt(3, employeeId)
            if (isAccepted != null) {
                preparedStatement.setBoolean(4, isAccepted)
                preparedStatement.setBoolean(5, isAccepted)
            } else {
                preparedStatement.setNull(4, java.sql.Types.BOOLEAN)
                preparedStatement.setNull(5, java.sql.Types.BOOLEAN)
            }
            preparedStatement.setInt(6, employeeId)

            val rowsAffected = preparedStatement.executeUpdate()
            preparedStatement.close()

            println("Update acceptance status affected $rowsAffected rows")
            rowsAffected > 0
        } catch (e: Exception) {
            println("Failed to update acceptance status: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun loadCanteens(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val canteens = fetchCanteens()
            _canteenCache.value = canteens.associate { it.canteenId to it.address }
            Result.success(Unit)
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка загрузки столовых: ${e.message}"
            Result.failure(e)
        }
    }

    fun getStatusName(statusId: Int): String {
        return _statusCache.value[statusId] ?: "Неизвестный статус"
    }

    fun getCanteenName(canteenId: Int): String {
        return _canteenCache.value[canteenId] ?: "Неизвестная столовая"
    }

    // Метод для регистрации нового пользователя
    suspend fun registerUser(
        surname: String,
        name: String,
        patronymic: String,
        phoneNumber: String,
        password: String
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            println("Регистрация пользователя: surname=$surname, name=$name, phoneNumber=$phoneNumber")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString) as Connection

            // Проверка, существует ли пользователь с таким номером телефона
            val checkQuery = "SELECT COUNT(*) FROM employees WHERE phoneNumber = ?"
            val checkStmt = connection.prepareStatement(checkQuery)
            checkStmt.setString(1, phoneNumber)
            val resultSet = checkStmt.executeQuery()
            resultSet.next()
            val count = resultSet.getInt(1)
            checkStmt.close()
            if (count > 0) {
                println("Пользователь с номером телефона $phoneNumber уже существует")
                return@withContext false
            }

            // Генерация соли и хэша пароля
            val salt = HashUtils.generateSalt()
            val hashedPassword = HashUtils.hashPassword(password, salt)
            println("Сгенерирована соль: $salt, хэш пароля: $hashedPassword")

            // Вставка нового сотрудника с автоинкрементом
            val insertQuery = """
                INSERT INTO employees (surname, name, patronymic, phoneNumber, password, salt, role)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            val insertStmt = connection.prepareStatement(insertQuery, java.sql.Statement.RETURN_GENERATED_KEYS)
            insertStmt.setString(1, surname)
            insertStmt.setString(2, name)
            insertStmt.setString(3, patronymic)
            insertStmt.setString(4, phoneNumber)
            insertStmt.setString(5, hashedPassword)
            insertStmt.setString(6, salt)
            insertStmt.setInt(7, 1) // Роль по умолчанию — 1 (обычный пользователь)
            val rowsAffected = insertStmt.executeUpdate()

            if (rowsAffected > 0) {
                val generatedKeys = insertStmt.generatedKeys
                var newEmployeeId = 0
                if (generatedKeys.next()) {
                    newEmployeeId = generatedKeys.getInt(1)
                }
                insertStmt.close()
                println("Пользователь успешно зарегистрирован с ID: $newEmployeeId")
                true
            } else {
                insertStmt.close()
                println("Ошибка при регистрации пользователя")
                false
            }
        } catch (e: Exception) {
            println("Ошибка регистрации: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun validateCredentials(): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            val query = "SELECT employeeID, password, salt, role FROM employees WHERE phoneNumber = ?"
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setString(1, _uiState.value.phoneNumber)
            val resultSet = preparedStatement.executeQuery()

            val isValid = if (resultSet.next()) {
                val employeeId = resultSet.getInt("employeeID")
                val storedHash = resultSet.getString("password")
                val storedSalt = resultSet.getString("salt")
                val role = resultSet.getInt("role")
                val inputPassword = _uiState.value.password
                val valid = HashUtils.verifyPassword(inputPassword, storedSalt, storedHash)
                if (valid) {
                    saveUserData(employeeId, _uiState.value.phoneNumber, role)
                    _userRole.value = role
                }
                valid
            } else {
                false
            }

            resultSet.close()
            preparedStatement.close()
            connection.close()
            isValid
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    private fun saveUserData(employeeId: Int, phoneNumber: String, role: Int) {
        with(prefs.edit()) {
            putInt("employeeId", employeeId)
            putString("phoneNumber", phoneNumber)
            putInt("role", role)
            putBoolean("isLoggedIn", true)
            apply()
        }
    }

    fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean("isLoggedIn", false)
    }

    fun getLoggedInPhoneNumber(): String? {
        return prefs.getString("phoneNumber", null)
    }

    fun getEmployeeId(): Int {
        val employeeID = prefs.getInt("employeeId", 0)
        println("getEmployeeId returned: $employeeID")
        return employeeID
    }

    fun getUserRole(): Int {
        return prefs.getInt("role", 1)
    }

    fun isAdmin(): Boolean {
        return getUserRole() == 2
    }

    fun logout() {
        with(prefs.edit()) {
            clear()
            apply()
        }
        _userRole.value = null
    }

    // Метод для поиска заказов
    suspend fun searchOrders(query: String): List<Order> = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        val orders = mutableListOf<Order>()
        try {
            println("Поиск заказов с запросом: $query")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val querySql = if (query.isNotEmpty()) {
                """
                SELECT 
                    o.orderID, 
                    o.creationDate, 
                    o.orderStatus, 
                    os.statusName
                FROM orders o
                LEFT JOIN orderStatus os ON o.orderStatus = os.statusID
                WHERE o.orderID LIKE ?
                ORDER BY o.creationDate DESC
                """
            } else {
                """
                SELECT 
                    o.orderID, 
                    o.creationDate, 
                    o.orderStatus, 
                    os.statusName
                FROM orders o
                LEFT JOIN orderStatus os ON o.orderStatus = os.statusID
                ORDER BY o.creationDate DESC
                """
            }

            val stmt = connection.prepareStatement(querySql)
            if (query.isNotEmpty()) {
                stmt.setString(1, "%$query%")
            }

            val resultSet = stmt.executeQuery()
            while (resultSet.next()) {
                orders.add(
                    Order(
                        orderId = resultSet.getInt("orderID"),
                        creationDate = resultSet.getString("creationDate") ?: "",
                        creationTime = "", // Заглушка
                        canteenSenderAddress = "", // Заглушка
                        canteenReceiverAddress = "", // Заглушка
                        statusId = resultSet.getInt("orderStatus"),
                        senderCanteen = Canteen(0, ""), // Заглушка
                        receiverCanteen = Canteen(0, ""), // Заглушка
                        status = OrderStatus(
                            statusId = resultSet.getInt("orderStatus"),
                            statusName = resultSet.getString("statusName") ?: "Не указано"
                        ),
                        isViewed = null // По умолчанию
                    )
                )
            }
            println("Получено заказов: ${orders.size}")
            resultSet.close()
            stmt.close()
        } catch (e: Exception) {
            println("Ошибка поиска заказов: ${e.message}")
            e.printStackTrace()
        } finally {
            connection?.close()
        }
        orders
    }

    // Метод для получения истории изменений статуса
    fun getOrderStatusHistory(orderId: Int): StateFlow<List<StatusChange>> {
        // Создаем или получаем существующий StateFlow для orderId
        val historyFlow = _historyCache.getOrPut(orderId) {
            MutableStateFlow(emptyList())
        }
        viewModelScope.launch {
            try {
                val history = fetchOrderStatusHistory(orderId)
                historyFlow.value = history
                println("History loaded for orderId=$orderId: ${history.size} changes")
            } catch (e: Exception) {
                println("Failed to load history for orderId=$orderId: ${e.message}")
                _errorMessage.value = "Ошибка загрузки истории: ${e.message}"
            }
        }
        return historyFlow.asStateFlow()
    }

    private suspend fun fetchOrderStatusHistory(orderId: Int): List<StatusChange> = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        val statusChanges = mutableListOf<StatusChange>()
        try {
            println("Fetching history for orderId=$orderId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = """
                SELECT changeID, orderID, oldStatusID, newStatusID, changedByCanteenID, changeTimestamp
                FROM orderStatusChanges
                WHERE orderID = ?
                ORDER BY changeTimestamp DESC
            """.trimIndent()

            val stmt = connection.prepareStatement(query)
            stmt.setInt(1, orderId)
            val resultSet = stmt.executeQuery()

            while (resultSet.next()) {
                statusChanges.add(
                    StatusChange(
                        changeId = resultSet.getInt("changeID"),
                        orderId = resultSet.getInt("orderID"),
                        oldStatusId = resultSet.getInt("oldStatusID"),
                        newStatusId = resultSet.getInt("newStatusID"),
                        changedByCanteenId = resultSet.getInt("changedByCanteenID"),
                        changeTimestamp = resultSet.getTimestamp("changeTimestamp")?.let {
                            LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault())
                        } ?: LocalDateTime.now()
                    )
                )
            }
            println("Fetched ${statusChanges.size} status changes for orderId=$orderId")
            resultSet.close()
            stmt.close()
        } catch (e: Exception) {
            println("Error fetching history for orderId=$orderId: ${e.message}")
            throw e
        } finally {
            connection?.close()
        }
        statusChanges
    }

    // Метод для обновления статуса заказа
    suspend fun updateOrderStatus(orderId: Int, newStatusId: Int, changedByCanteenId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Updating order status for orderId: $orderId, newStatusId: $newStatusId, changedByCanteenId: $changedByCanteenId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            // Проверка для статуса "Исполнен" (statusId = 3)
            if (newStatusId == 3) {
                val checkQuery = """
                    SELECT COUNT(*) FROM actualSentQuantities 
                    WHERE orderID = ? AND isAccepted IS NULL
                """.trimIndent()
                val checkStmt = connection.prepareStatement(checkQuery)
                checkStmt.setInt(1, orderId)
                val resultSet = checkStmt.executeQuery()
                resultSet.next()
                val unacceptedCount = resultSet.getInt(1)
                resultSet.close()
                checkStmt.close()

                if (unacceptedCount > 0) {
                    println("Cannot set status to Исполнен: $unacceptedCount products are not marked as accepted or rejected")
                    _errorMessage.value = "Все продукты должны быть отмечены как принятые или непринятые"
                    return@withContext false
                }
            }

            // Получение текущего статуса
            val currentStatusQuery = "SELECT orderStatus FROM orders WHERE orderID = ?"
            val currentStmt = connection.prepareStatement(currentStatusQuery)
            currentStmt.setInt(1, orderId)
            val resultSet = currentStmt.executeQuery()
            if (!resultSet.next()) {
                println("Order with orderId $orderId not found")
                resultSet.close()
                currentStmt.close()
                return@withContext false
            }
            val oldStatusId = resultSet.getInt("orderStatus")
            resultSet.close()
            currentStmt.close()

            // Обновление статуса
            val updateQuery = "UPDATE orders SET orderStatus = ? WHERE orderID = ?"
            val updateStmt = connection.prepareStatement(updateQuery)
            updateStmt.setInt(1, newStatusId)
            updateStmt.setInt(2, orderId)
            val rowsAffected = updateStmt.executeUpdate()
            updateStmt.close()

            if (rowsAffected > 0) {
                println("Order status updated successfully. Old status: $oldStatusId, New status: $newStatusId")
                // Логирование изменения статуса
                logStatusChange(orderId, oldStatusId, newStatusId, changedByCanteenId)

                // Обновление списка заказов в UI
                _orders.value = _orders.value.map { order ->
                    if (order.orderId == orderId) {
                        val updatedStatus = order.status.copy(statusId = newStatusId, statusName = getStatusName(newStatusId))
                        order.copy(status = updatedStatus)
                    } else {
                        order
                    }
                }
                true
            } else {
                println("Failed to update order status: No rows affected")
                false
            }
        } catch (e: Exception) {
            println("Failed to update order status: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun isEmployeeFromCustomerCanteen(orderId: Int, employeeId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Checking if employee $employeeId is from customer canteen for orderId: $orderId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = """
                SELECT o.canteenCustomerID, e.canteen
                FROM orders o
                LEFT JOIN employees e ON e.employeeID = ?
                WHERE o.orderID = ?
            """.trimIndent()

            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, employeeId)
            preparedStatement.setInt(2, orderId)
            val resultSet = preparedStatement.executeQuery()

            val isFromCustomerCanteen = if (resultSet.next()) {
                val canteenCustomerId = resultSet.getInt("canteenCustomerID")
                val employeeCanteenId = resultSet.getInt("canteen")
                val isCanteenNull = resultSet.wasNull()
                println("CanteenCustomerID: $canteenCustomerId, EmployeeCanteenID: $employeeCanteenId, isCanteenNull: $isCanteenNull")
                !isCanteenNull && canteenCustomerId == employeeCanteenId
            } else {
                false
            }

            resultSet.close()
            preparedStatement.close()
            println("isEmployeeFromCustomerCanteen result: $isFromCustomerCanteen")
            isFromCustomerCanteen
        } catch (e: Exception) {
            println("Failed to check employee canteen: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    // Метод для получения списка статусов
    suspend fun fetchStatuses(): List<OrderStatus> = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            val query = "SELECT statusID, statusName FROM orderStatus"
            val stmt = connection.prepareStatement(query)
            val resultSet = stmt.executeQuery()
            val statuses = mutableListOf<OrderStatus>()
            while (resultSet.next()) {
                statuses.add(
                    OrderStatus(
                        statusId = resultSet.getInt("statusID"),
                        statusName = resultSet.getString("statusName")
                    )
                )
            }
            statuses
        } catch (e: Exception) {
            println("Failed to fetch statuses: ${e.message}")
            emptyList()
        } finally {
            connection?.close()
        }
    }
    // Метод для записи изменения статуса в таблицу orderStatusChanges
    private suspend fun logStatusChange(orderId: Int, oldStatusId: Int, newStatusId: Int, changedByCanteenId: Int) = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            val employeeId = getEmployeeId() // Получаем ID текущего сотрудника
            println("Logging status change for orderId: $orderId, oldStatusId: $oldStatusId, newStatusId: $newStatusId, changedByCanteenId: $changedByCanteenId, changedByEmployeeId: $employeeId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val insertQuery = """
            INSERT INTO orderStatusChanges (orderID, oldStatusID, newStatusID, changedByCanteenID, changedByEmployeeID, changeTimestamp)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()
            val insertStmt = connection.prepareStatement(insertQuery)
            insertStmt.setInt(1, orderId)
            insertStmt.setInt(2, oldStatusId)
            insertStmt.setInt(3, newStatusId)
            insertStmt.setInt(4, changedByCanteenId)
            insertStmt.setInt(5, employeeId) // Новое поле changedByEmployeeID
            insertStmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
            val rowsAffected = insertStmt.executeUpdate()
            insertStmt.close()

            if (rowsAffected > 0) {
                println("Status change logged successfully")
            } else {
                println("Failed to log status change: No rows affected")
            }
        } catch (e: Exception) {
            println("Failed to log status change: ${e.message}")
            e.printStackTrace()
        } finally {
            connection?.close()
        }
    }

    // Получение названия единицы измерения по ID
    suspend fun getMeasurementName(measurementId: Int): String = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            val query = "SELECT measurementName FROM unitsOfMeasurement WHERE measurementID = ?"
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, measurementId)
            val resultSet = preparedStatement.executeQuery()
            
            val measurementName = if (resultSet.next()) {
                resultSet.getString("measurementName") ?: "ед."
            } else {
                "ед."
            }
            
            resultSet.close()
            preparedStatement.close()
            measurementName
        } catch (e: Exception) {
            println("Failed to fetch measurement name: ${e.message}")
            e.printStackTrace()
            "ед." // Возвращаем значение по умолчанию в случае ошибки
        } finally {
            connection?.close()
        }
    }

    fun updatePhoneNumber(newPhoneNumber: String) {
        _uiState.update { it.copy(phoneNumber = newPhoneNumber) }
    }

    fun updatePassword(newPassword: String) {
        _uiState.update { it.copy(password = newPassword) }
    }

    suspend fun fetchOrders(
        sortOrder: SortOrder,
        senderCanteenId: Int? = null,
        receiverCanteenId: Int? = null,
        startDate: String? = null,
        endDate: String? = null,
        statusId: Int? = null
    ) = withContext(Dispatchers.IO) {
        val ordersList = mutableListOf<Order>()
        val viewStatusMap = mutableMapOf<Int, Boolean>()
        var connection: java.sql.Connection? = null
        try {
            val employeeId = getEmployeeId()
            println("Fetching orders for employeeID: $employeeId with sortOrder: $sortOrder, senderCanteenId: $senderCanteenId, receiverCanteenId: $receiverCanteenId, startDate: $startDate, endDate: $endDate, statusId: $statusId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            println("Connected to database: $connectionString")

            val conditions = mutableListOf<String>()
            val parameters = mutableListOf<Any?>()

            if (senderCanteenId != null) {
                conditions.add("o.canteenCustomerID = ?")
                parameters.add(senderCanteenId)
            }
            if (receiverCanteenId != null) {
                conditions.add("o.canteenExecutorID = ?")
                parameters.add(receiverCanteenId)
            }
            if (startDate != null) {
                try {
                    val parsedStartDate = java.time.LocalDate.parse(startDate)
                    val startDateString = parsedStartDate.toString()
                    val sqlStartDate = java.sql.Date.valueOf(startDateString)
                    if (endDate != null) {
                        val parsedEndDate = java.time.LocalDate.parse(endDate)
                        val endDateString = parsedEndDate.toString()
                        val sqlEndDate = java.sql.Date.valueOf(endDateString)
                        conditions.add("o.creationDate BETWEEN ? AND ?")
                        parameters.add(sqlStartDate)
                        parameters.add(sqlEndDate)
                    } else {
                        conditions.add("o.creationDate >= ?")
                        parameters.add(sqlStartDate)
                    }
                } catch (e: Exception) {
                    println("Invalid date format: startDate=$startDate, endDate=$endDate, error: ${e.message}")
                    throw IllegalArgumentException("Неверный формат даты. Ожидается формат гггг-мм-дд.")
                }
            } else if (endDate != null) {
                try {
                    val parsedEndDate = java.time.LocalDate.parse(endDate)
                    val endDateString = parsedEndDate.toString()
                    val sqlEndDate = java.sql.Date.valueOf(endDateString)
                    conditions.add("o.creationDate <= ?")
                    parameters.add(sqlEndDate)
                } catch (e: Exception) {
                    println("Invalid date format: endDate=$endDate, error: ${e.message}")
                    throw IllegalArgumentException("Неверный формат даты. Ожидается формат гггг-мм-дд.")
                }
            }
            if (statusId != null) {
                conditions.add("o.orderStatus = ?")
                parameters.add(statusId)
            }

            val whereClause = if (conditions.isNotEmpty()) {
                "WHERE ${conditions.joinToString(" AND ")}"
            } else {
                ""
            }

            val orderByClause = when (sortOrder) {
                SortOrder.NEWEST_FIRST -> "ORDER BY o.creationDate DESC, o.creationTime DESC"
                SortOrder.OLDEST_FIRST -> "ORDER BY o.creationDate ASC, o.creationTime ASC"
            }

            val query = """
            SELECT o.orderID, o.creationDate, o.creationTime, o.orderStatus,
                   sender.canteenID AS SenderCanteenID, sender.canteenAddress AS SenderAddress,
                   receiver.canteenID AS ReceiverCanteenID, receiver.canteenAddress AS ReceiverAddress,
                   os.statusName AS StatusName,
                   ov.isViewed
            FROM orders o
            LEFT JOIN canteens sender ON o.canteenCustomerID = sender.canteenID
            LEFT JOIN canteens receiver ON o.canteenExecutorID = receiver.canteenID
            LEFT JOIN orderStatus os ON o.orderStatus = os.statusID
            LEFT JOIN orderViews ov ON o.orderID = ov.orderID AND ov.employeeID = ?
            $whereClause
            $orderByClause
        """.trimIndent()

            println("Executing query: $query")
            println("Parameters: [employeeID=$employeeId, $parameters]")

            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, employeeId)
            parameters.forEachIndexed { index, param ->
                println("Setting parameter at index ${index + 2}: $param (type: ${param?.javaClass?.simpleName})")
                when (param) {
                    is Int -> preparedStatement.setInt(index + 2, param)
                    is java.sql.Date -> preparedStatement.setDate(index + 2, param)
                    else -> throw IllegalArgumentException("Unsupported parameter type: $param")
                }
            }

            val resultSet = preparedStatement.executeQuery()

            var count = 0
            while (resultSet.next()) {
                count++
                val rawCreationDate = resultSet.getDate("creationDate")
                val creationDateString = resultSet.getString("creationDate") ?: "Не указано"
                val orderId = resultSet.getInt("orderID")
                println("Raw creationDate for order $orderId: $rawCreationDate (string: $creationDateString)")
                val order = Order(
                    orderId = orderId,
                    creationDate = creationDateString,
                    creationTime = resultSet.getString("creationTime") ?: "Не указано",
                    canteenSenderAddress = resultSet.getString("SenderAddress") ?: "Неизвестно",
                    canteenReceiverAddress = resultSet.getString("ReceiverAddress") ?: "Неизвестно",
                    statusId = resultSet.getInt("orderStatus"),
                    senderCanteen = Canteen(
                        canteenId = resultSet.getInt("SenderCanteenID"),
                        address = resultSet.getString("SenderAddress") ?: "Неизвестно"
                    ),
                    receiverCanteen = Canteen(
                        canteenId = resultSet.getInt("ReceiverCanteenID"),
                        address = resultSet.getString("ReceiverAddress") ?: "Неизвестно"
                    ),
                    status = OrderStatus(
                        statusId = resultSet.getInt("orderStatus"),
                        statusName = resultSet.getString("StatusName") ?: "Неизвестно"
                    )
                )
                // Загружаем статус просмотра (null интерпретируется как false)
                viewStatusMap[orderId] = resultSet.getBoolean("isViewed") && !resultSet.wasNull()
                println("Fetched order: $order, isViewed: ${viewStatusMap[orderId]}")
                ordersList.add(order)
            }
            println("Total orders fetched: $count")

            resultSet.close()
            preparedStatement.close()
        } catch (e: Exception) {
            println("Failed to fetch orders: ${e.message}")
            e.printStackTrace()
            _orders.value = emptyList()
            _orderViewStatus.value = emptyMap()
            throw e
        } finally {
            connection?.close()
        }
        _orders.value = ordersList
        _orderViewStatus.value = viewStatusMap
    }

    fun loadOrders(
        sortOrder: SortOrder,
        senderCanteenId: Int? = null,
        receiverCanteenId: Int? = null,
        startDate: String? = null,
        endDate: String? = null,
        statusId: Int? = null
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                println("Starting to fetch orders with filters: senderCanteenId=$senderCanteenId, receiverCanteenId=$receiverCanteenId, startDate=$startDate, endDate=$endDate, statusId=$statusId, sortOrder=$sortOrder")
                withTimeoutOrNull(10000L) {
                    fetchOrders(sortOrder, senderCanteenId, receiverCanteenId, startDate, endDate, statusId)
                } ?: run {
                    _errorMessage.value = "Превышено время ожидания загрузки заказов"
                    println("Fetch orders timed out")
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("Неверный формат даты") == true -> e.message
                    else -> "Не удалось загрузить заказы: ${e.message}"
                }
                _errorMessage.value = errorMsg
                println("Error loading orders: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                println("Finished loading orders, isLoading: ${_isLoading.value}, orders size: ${_orders.value.size}")
            }
        }
    }

    suspend fun markOrderAsViewed(orderId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            val employeeId = getEmployeeId()
            println("Marking order as viewed: input orderId=$orderId, input employeeID=$employeeId (from getEmployeeId)")
            if (employeeId <= 0) {
                println("Invalid employeeId: $employeeId")
                return@withContext false
            }

            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            connection.autoCommit = false // Начало транзакции

            // Проверяем, существует ли запись
            val checkQuery = "SELECT isViewed FROM orderViews WHERE employeeID = ? AND orderID = ?"
            val checkStatement = connection.prepareStatement(checkQuery)
            checkStatement.setInt(1, employeeId)
            checkStatement.setInt(2, orderId)
            val resultSet = checkStatement.executeQuery()
            val exists = resultSet.next()
            val currentIsViewed = if (exists) resultSet.getBoolean("isViewed") else false
            resultSet.close()
            checkStatement.close()
            println("Check result: exists=$exists, currentIsViewed=$currentIsViewed, employeeId=$employeeId, orderId=$orderId")

            // Используем ON DUPLICATE KEY UPDATE для избежания ошибки дублирования
            val query = """
            INSERT INTO orderViews (employeeID, orderID, isViewed)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE isViewed = ?
        """.trimIndent()
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, employeeId)
            preparedStatement.setInt(2, orderId)
            preparedStatement.setBoolean(3, true)
            preparedStatement.setBoolean(4, true)
            println("Executing query: $query with bound values - employeeId=$employeeId, orderId=$orderId")
            val rowsAffected = preparedStatement.executeUpdate()
            preparedStatement.close()
            println("Query executed, rowsAffected=$rowsAffected")

            // Обновляем состояние независимо от того, была ли вставка или обновление
            if (rowsAffected > 0 || (exists && currentIsViewed)) {
                _orderViewStatus.value = _orderViewStatus.value.toMap().toMutableMap().apply {
                    this[orderId] = true
                }.toMap()
                connection.commit()
                true
            } else {
                connection.rollback()
                false
            }
        } catch (e: Exception) {
            println("Failed to mark order as viewed: ${e.message}")
            e.printStackTrace()
            connection?.rollback()
            false
        } finally {
            connection?.close()
        }
    }

    // Асинхронный метод для вызова из UI
    fun markOrderAsViewedAsync(orderId: Int) {
        viewModelScope.launch {
            val success = markOrderAsViewed(orderId)
            println("markOrderAsViewed result for orderId=$orderId: success=$success")
        }
    }

    suspend fun fetchEmployeeProfile(): Employee? = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            val query = "SELECT employeeID, name, surname, patronymic, phoneNumber, role FROM employees WHERE employeeID = ?"
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, getEmployeeId())
            val resultSet = preparedStatement.executeQuery()

            val employee = if (resultSet.next()) {
                val name = resultSet.getString("name")?.takeIf { it.isNotEmpty() } ?: ""
                val surname = resultSet.getString("surname")?.takeIf { it.isNotEmpty() } ?: ""
                val patronymic = resultSet.getString("patronymic")?.takeIf { it.isNotEmpty() } ?: ""
                val phoneNumber = resultSet.getString("phoneNumber")?.takeIf { it.isNotEmpty() } ?: ""
                val role = resultSet.getInt("role") // Извлекаем role
                println("Fetched employee data: name=$name, surname=$surname, patronymic=$patronymic, phoneNumber=$phoneNumber, role=$role")
                Employee(
                    employeeId = resultSet.getInt("employeeID"),
                    name = name,
                    surname = surname,
                    patronymic = patronymic,
                    phoneNumber = phoneNumber,
                    role = role // Передаём role
                )
            } else {
                null
            }

            resultSet.close()
            preparedStatement.close()
            employee
        } catch (e: Exception) {
            println("Failed to fetch employee profile: ${e.message}")
            e.printStackTrace()
            null
        } finally {
            connection?.close()
        }
    }

    suspend fun updateEmployeeProfile(employee: Employee): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Updating employee profile: employeeId=${employee.employeeId}, name=${employee.name}, surname=${employee.surname}, phoneNumber=${employee.phoneNumber}, role=${employee.role}")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = """
            UPDATE employees 
            SET name = ?, surname = ?, patronymic = ?, phoneNumber = ?, role = ?
            WHERE employeeID = ?
        """.trimIndent()
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setString(1, employee.name)
            preparedStatement.setString(2, employee.surname)
            preparedStatement.setString(3, employee.patronymic)
            preparedStatement.setString(4, employee.phoneNumber)
            preparedStatement.setInt(5, employee.role)
            preparedStatement.setInt(6, employee.employeeId)
            val rowsAffected = preparedStatement.executeUpdate()

            preparedStatement.close()
            if (rowsAffected > 0) {
                // Обновляем StateFlow, если используется список сотрудников
                _employees.value = _employees.value.map {
                    if (it.employeeId == employee.employeeId) employee else it
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to update employee profile: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun fetchCanteens(): List<Canteen> = withContext(Dispatchers.IO) {
        // Проверяем, есть ли данные в кэше
        if (_canteenCache.value.isNotEmpty()) {
            println("Returning cached canteens: ${_canteenCache.value.size}")
            return@withContext _canteenCache.value.map { (id, address) ->
                Canteen(canteenId = id, address = address)
            }
        }

        var connection: java.sql.Connection? = null
        val canteensList = mutableListOf<Canteen>()
        try {
            println("Fetching canteens from database")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            val query = "SELECT canteenID, canteenAddress FROM canteens"
            val preparedStatement = connection.prepareStatement(query)
            val resultSet = preparedStatement.executeQuery()

            val cache = mutableMapOf<Int, String>()
            while (resultSet.next()) {
                val id = resultSet.getInt("canteenID")
                val address = resultSet.getString("canteenAddress")?.takeIf { it.isNotEmpty() } ?: "Неизвестно"
                canteensList.add(Canteen(canteenId = id, address = address))
                cache[id] = address
            }

            _canteenCache.value = cache
            _canteens.value = canteensList // Синхронизируем _canteens
            println("Fetched canteens: ${canteensList.size}")

            resultSet.close()
            preparedStatement.close()
        } catch (e: Exception) {
            println("Failed to fetch canteens: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            try {
                connection?.close()
            } catch (e: Exception) {
                println("Error closing connection: ${e.message}")
            }
        }
        canteensList
    }

    suspend fun fetchProducts(): List<Product> = withContext(Dispatchers.IO) {
        val productsList = mutableListOf<Product>()
        var connection: java.sql.Connection? = null
        try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            val query = """
                SELECT p.productID, p.productName, p.priceOfUnit, p.unitOfMeasurement, u.measurementName
                FROM products p
                LEFT JOIN unitsOfMeasurement u ON p.unitOfMeasurement = u.measurementID
            """.trimIndent()
            val preparedStatement = connection.prepareStatement(query)
            val resultSet = preparedStatement.executeQuery()

            while (resultSet.next()) {
                val product = Product(
                    productId = resultSet.getInt("productID"),
                    name = resultSet.getString("productName")?.takeIf { it.isNotEmpty() } ?: "Неизвестно",
                    priceOfUnit = resultSet.getDouble("priceOfUnit"),
                    unitOfMeasurement = resultSet.getString("measurementName")?.takeIf { it.isNotEmpty() } ?: "Не указано"
                )
                productsList.add(product)
            }

            resultSet.close()
            preparedStatement.close()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            connection?.close()
        }
        _products.value = productsList
        productsList
    }
    suspend fun deleteProduct(productId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Deleting product with productId: $productId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            // Удаляем продукт из таблицы products
            val query = "DELETE FROM products WHERE productID = ?"
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, productId)
            val rowsAffected = preparedStatement.executeUpdate()

            preparedStatement.close()

            // Обновляем список продуктов в UI
            if (rowsAffected > 0) {
                _products.value = _products.value.filter { it.productId != productId }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to delete product: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }
    suspend fun fetchUnitsOfMeasurement(): List<UnitOfMeasurement> = withContext(Dispatchers.IO) {
        val unitsList = mutableListOf<UnitOfMeasurement>()
        var connection: java.sql.Connection? = null
        try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = "SELECT measurementID, measurementName FROM unitsOfMeasurement"
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(query)

            while (resultSet.next()) {
                val unit = UnitOfMeasurement(
                    measurementId = resultSet.getInt("measurementID"),
                    measurementName = resultSet.getString("measurementName")?.takeIf { it.isNotEmpty() } ?: "Не указано"
                )
                unitsList.add(unit)
            }

            resultSet.close()
            statement.close()
            _unitsOfMeasurement.value = unitsList
        } catch (e: Exception) {
            println("Failed to fetch units of measurement: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            connection?.close()
        }
        unitsList
    }

    suspend fun addProduct(name: String, priceOfUnit: Double, unitOfMeasurementId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            println("Adding product: name=$name, priceOfUnit=$priceOfUnit, unitOfMeasurementId=$unitOfMeasurementId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString) as Connection

            // Вставка нового продукта с автоинкрементом
            val query = "INSERT INTO products (productName, priceOfUnit, unitOfMeasurement) VALUES (?, ?, ?)"
            val preparedStatement = connection.prepareStatement(query, java.sql.Statement.RETURN_GENERATED_KEYS)
            preparedStatement.setString(1, name)
            preparedStatement.setDouble(2, priceOfUnit)
            preparedStatement.setInt(3, unitOfMeasurementId)
            val rowsAffected = preparedStatement.executeUpdate()
            val generatedKeys = preparedStatement.generatedKeys
            val newProductId = if (generatedKeys.next()) generatedKeys.getInt(1) else -1
            preparedStatement.close()

            if (rowsAffected > 0 && newProductId != -1) {
                _products.value = _products.value + Product(
                    productId = newProductId,
                    name = name,
                    priceOfUnit = priceOfUnit,
                    unitOfMeasurement = unitsOfMeasurement.value.find { it.measurementId == unitOfMeasurementId }?.measurementName ?: "Не указано"
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to add product: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun updateProduct(productId: Int, name: String, priceOfUnit: Double, unitOfMeasurementId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Updating product: productId=$productId, name=$name, priceOfUnit=$priceOfUnit, unitOfMeasurementId=$unitOfMeasurementId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = """
            UPDATE products 
            SET productName = ?, priceOfUnit = ?, unitOfMeasurement = ?
            WHERE productID = ?
        """.trimIndent()
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setString(1, name)
            preparedStatement.setDouble(2, priceOfUnit)
            preparedStatement.setInt(3, unitOfMeasurementId)
            preparedStatement.setInt(4, productId)
            val rowsAffected = preparedStatement.executeUpdate()

            preparedStatement.close()
            if (rowsAffected > 0) {
                // Обновляем список продуктов в UI
                _products.value = _products.value.map {
                    if (it.productId == productId) {
                        it.copy(
                            name = name,
                            priceOfUnit = priceOfUnit,
                            unitOfMeasurement = unitsOfMeasurement.value.find { unit -> unit.measurementId == unitOfMeasurementId }?.measurementName ?: it.unitOfMeasurement
                        )
                    } else {
                        it
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to update product: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun createOrder(
        canteenCustomerId: Int,
        canteenExecutorId: Int,
        products: List<OrderProduct>
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            val employeeId = getEmployeeId()
            println("Attempting to create order with: canteenCustomerId=$canteenCustomerId, canteenExecutorId=$canteenExecutorId, employeeId=$employeeId, products=$products")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString) as Connection
            connection.autoCommit = false

            val summaryPrice = products.sumOf { it.quantity * it.product.priceOfUnit }
            println("Calculated summaryPrice: $summaryPrice")

            // Вставка заказа с автоинкрементом
            val orderQuery = """
                INSERT INTO orders (creationDate, creationTime, executionDate, executionTime, canteenCustomerID, canteenExecutorID, employeeCustomerID, employeeExecutorID, orderStatus, summaryPrice)
                VALUES (NOW(), NOW(), NOW(), NOW(), ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            val orderStatement = connection.prepareStatement(orderQuery, java.sql.Statement.RETURN_GENERATED_KEYS)
            orderStatement.setInt(1, canteenCustomerId)
            orderStatement.setInt(2, canteenExecutorId)
            orderStatement.setInt(3, employeeId)
            orderStatement.setInt(4, employeeId)
            orderStatement.setInt(5, 1) // orderStatus
            orderStatement.setDouble(6, summaryPrice)
            orderStatement.executeUpdate()
            val generatedKeys = orderStatement.generatedKeys
            val newOrderId = if (generatedKeys.next()) generatedKeys.getInt(1) else -1
            orderStatement.close()
            println("Order created with new orderID: $newOrderId")

            if (newOrderId == -1) return@withContext false

            // Добавляем запись в orderViews
            val viewQuery = "INSERT INTO orderViews (employeeID, orderID, isViewed) VALUES (?, ?, ?)"
            val viewStatement = connection.prepareStatement(viewQuery)
            viewStatement.setInt(1, employeeId)
            viewStatement.setInt(2, newOrderId)
            viewStatement.setBoolean(3, false)
            viewStatement.executeUpdate()
            viewStatement.close()
            println("Added order view for employeeID=$employeeId, orderID=$newOrderId, isViewed=false")

            // Вставка orderedItems и orderComposition
            val itemQuery = """
                INSERT INTO orderedItems (product, quantity, summaryPrice)
                VALUES (?, ?, ?)
            """.trimIndent()
            val itemStatement = connection.prepareStatement(itemQuery, java.sql.Statement.RETURN_GENERATED_KEYS)

            val compositionQuery = """
                INSERT INTO orderComposition (orderID, orderedItem, summaryPrice)
                VALUES (?, ?, ?)
            """.trimIndent()
            val compositionStatement = connection.prepareStatement(compositionQuery)

            for (orderProduct in products) {
                val itemSummaryPrice = orderProduct.quantity * orderProduct.product.priceOfUnit
                itemStatement.setInt(1, orderProduct.product.productId)
                itemStatement.setDouble(2, orderProduct.quantity)
                itemStatement.setDouble(3, itemSummaryPrice)
                itemStatement.executeUpdate()
                val itemKeys = itemStatement.generatedKeys
                val newItemId = if (itemKeys.next()) itemKeys.getInt(1) else -1
                itemKeys.close()

                if (newItemId != -1) {
                    compositionStatement.setInt(1, newOrderId)
                    compositionStatement.setInt(2, newItemId)
                    compositionStatement.setDouble(3, itemSummaryPrice)
                    compositionStatement.executeUpdate()
                }
            }

            itemStatement.close()
            compositionStatement.close()
            connection.commit()
            println("Order created successfully with ID: $newOrderId")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to create order: ${e.message}")
            connection?.rollback()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun fetchOrderDetails(orderId: Int): OrderDetails = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Fetching order details for orderId: $orderId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            println("Connected to database: $connectionString")

            val orderQuery = """
            SELECT o.orderID, o.creationDate, o.creationTime, 
                   sender.canteenAddress AS SenderAddress, 
                   receiver.canteenAddress AS ReceiverAddress,
                   o.orderStatus, os.statusName
            FROM orders o
            LEFT JOIN canteens sender ON o.canteenCustomerID = sender.canteenID
            LEFT JOIN canteens receiver ON o.canteenExecutorID = receiver.canteenID
            LEFT JOIN orderStatus os ON o.orderStatus = os.statusID
            WHERE o.orderID = ?
        """.trimIndent()
            val orderStatement = connection.prepareStatement(orderQuery)
            orderStatement.setInt(1, orderId)
            val orderResultSet = orderStatement.executeQuery()

            if (!orderResultSet.next()) {
                throw Exception("Order with ID $orderId not found")
            }

            val statusId = orderResultSet.getInt("orderStatus")
            val statusName = orderResultSet.getString("statusName") ?: "Неизвестный статус"

            val orderDetails = OrderDetails(
                orderId = orderResultSet.getInt("orderID"),
                creationDate = orderResultSet.getString("creationDate") ?: "Не указано",
                creationTime = orderResultSet.getString("creationTime") ?: "Не указано",
                canteenSenderAddress = orderResultSet.getString("SenderAddress") ?: "Неизвестно",
                canteenReceiverAddress = orderResultSet.getString("ReceiverAddress") ?: "Неизвестно",
                products = mutableListOf(),
                status = OrderStatus(statusId = statusId, statusName = statusName)
            )

            orderResultSet.close()
            orderStatement.close()

            val productsQuery = """
            SELECT p.productID, p.productName, p.unitOfMeasurement, p.priceOfUnit, oi.quantity
            FROM orderComposition oc
            JOIN orderedItems oi ON oc.orderedItem = oi.itemID
            JOIN products p ON oi.product = p.productID
            WHERE oc.orderID = ?
        """.trimIndent()
            val productsStatement = connection.prepareStatement(productsQuery)
            productsStatement.setInt(1, orderId)
            val productsResultSet = productsStatement.executeQuery()

            val productsList = mutableListOf<OrderProduct>()
            while (productsResultSet.next()) {
                val product = Product(
                    productId = productsResultSet.getInt("productID"),
                    name = productsResultSet.getString("productName")?.takeIf { it.isNotEmpty() } ?: "Неизвестно",
                    unitOfMeasurement = productsResultSet.getString("unitOfMeasurement")?.takeIf { it.isNotEmpty() } ?: "Не указано",
                    priceOfUnit = productsResultSet.getDouble("priceOfUnit")
                )
                val orderProduct = OrderProduct(
                    product = product,
                    quantity = productsResultSet.getDouble("quantity")
                )
                productsList.add(orderProduct)
                println("Fetched product for order $orderId: $orderProduct")
            }

            productsResultSet.close()
            productsStatement.close()

            orderDetails.copy(products = productsList)
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to fetch order details: ${e.message}")
            throw e
        } finally {
            connection?.close()
        }
    }

    suspend fun fetchOrderStatuses(): List<OrderStatus> = withContext(Dispatchers.IO) {
        val statusesList = mutableListOf<OrderStatus>()
        var connection: java.sql.Connection? = null
        try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            val query = "SELECT statusID, statusName FROM orderStatus"
            val preparedStatement = connection.prepareStatement(query)
            val resultSet = preparedStatement.executeQuery()

            while (resultSet.next()) {
                val status = OrderStatus(
                    statusId = resultSet.getInt("statusID"),
                    statusName = resultSet.getString("statusName")?.takeIf { it.isNotEmpty() } ?: "Неизвестно"
                )
                statusesList.add(status)
            }

            resultSet.close()
            preparedStatement.close()
            _statuses.value = statusesList // Обновляем StateFlow
        } catch (e: Exception) {
            println("Failed to fetch statuses: ${e.message}")
            e.printStackTrace()
        } finally {
            connection?.close()
        }
        statusesList
    }

    private fun saveUserData(employeeId: Int, phoneNumber: String) {
        with(prefs.edit()) {
            putInt("employeeId", employeeId)
            putString("phoneNumber", phoneNumber)
            putBoolean("isLoggedIn", true)
            apply()
        }
    }

    suspend fun updateOrder(
        orderId: Int,
        canteenCustomerId: Int,
        canteenExecutorId: Int,
        products: List<OrderProduct>
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Updating order with: orderId=$orderId, canteenCustomerId=$canteenCustomerId, canteenExecutorId=$canteenExecutorId, products=$products")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            println("Connected to database: $connectionString")
            connection.autoCommit = false

            val summaryPrice = products.sumOf { it.quantity * it.product.priceOfUnit }
            println("Calculated summaryPrice: $summaryPrice")

            val orderQuery = """
                UPDATE orders 
                SET canteenCustomerID = ?, canteenExecutorID = ?, summaryPrice = ?
                WHERE orderID = ?
            """.trimIndent()
            val orderStatement = connection.prepareStatement(orderQuery)
            orderStatement.setInt(1, canteenCustomerId)
            orderStatement.setInt(2, canteenExecutorId)
            orderStatement.setDouble(3, summaryPrice)
            orderStatement.setInt(4, orderId)
            orderStatement.executeUpdate()
            orderStatement.close()
            println("Updated order with orderID: $orderId")

            val deleteCompositionQuery = "DELETE FROM orderComposition WHERE orderID = ?"
            val deleteCompositionStatement = connection.prepareStatement(deleteCompositionQuery)
            deleteCompositionStatement.setInt(1, orderId)
            deleteCompositionStatement.executeUpdate()
            deleteCompositionStatement.close()
            println("Deleted old orderComposition entries for orderID: $orderId")

            val deleteItemsQuery = """
                DELETE FROM orderedItems 
                WHERE itemID NOT IN (SELECT orderedItem FROM orderComposition)
            """.trimIndent()
            val deleteItemsStatement = connection.prepareStatement(deleteItemsQuery)
            deleteItemsStatement.executeUpdate()
            deleteItemsStatement.close()
            println("Deleted unused orderedItems")

            val maxItemIdQuery = "SELECT MAX(itemID) FROM orderedItems"
            val maxItemIdStatement = connection.prepareStatement(maxItemIdQuery)
            val itemResultSet = maxItemIdStatement.executeQuery()
            var nextItemId = if (itemResultSet.next()) (itemResultSet.getInt(1) + 1) else 1
            itemResultSet.close()
            maxItemIdStatement.close()
            println("Starting next item ID: $nextItemId")

            val maxOrderCompositionIdQuery = "SELECT MAX(compositionId) FROM orderComposition"
            val maxOrderCompositionIdStatement = connection.prepareStatement(maxOrderCompositionIdQuery)
            val orderCompositionResultSet = maxOrderCompositionIdStatement.executeQuery()
            var nextOrderCompositionId = if (orderCompositionResultSet.next()) (orderCompositionResultSet.getInt(1) + 1) else 1
            orderCompositionResultSet.close()
            maxOrderCompositionIdStatement.close()
            println("Starting next order composition ID: $nextOrderCompositionId")

            val itemQuery = """
                INSERT INTO orderedItems (itemID, product, quantity, summaryPrice)
                VALUES (?, ?, ?, ?)
            """.trimIndent()
            val itemStatement = connection.prepareStatement(itemQuery)

            val compositionQuery = """
                INSERT INTO orderComposition (compositionId, orderID, orderedItem, summaryPrice)
                VALUES (?, ?, ?, ?)
            """.trimIndent()
            val compositionStatement = connection.prepareStatement(compositionQuery)

            for (orderProduct in products) {
                val itemSummaryPrice = orderProduct.quantity * orderProduct.product.priceOfUnit
                println("Adding item: itemID=$nextItemId, product=${orderProduct.product.productId}, quantity=${orderProduct.quantity}, summaryPrice=$itemSummaryPrice")

                itemStatement.setInt(1, nextItemId)
                itemStatement.setInt(2, orderProduct.product.productId)
                itemStatement.setDouble(3, orderProduct.quantity)
                itemStatement.setDouble(4, itemSummaryPrice)
                itemStatement.executeUpdate()

                compositionStatement.setInt(1, nextOrderCompositionId)
                compositionStatement.setInt(2, orderId)
                compositionStatement.setInt(3, nextItemId)
                compositionStatement.setDouble(4, itemSummaryPrice)
                compositionStatement.executeUpdate()
                println("Added to orderComposition: compositionId=$nextOrderCompositionId, orderID=$orderId, itemID=$nextItemId")

                nextItemId++
                nextOrderCompositionId++
            }

            itemStatement.close()
            compositionStatement.close()

            // Удаляем все просмотры для этого заказа
            val deleteViewsQuery = "DELETE FROM orderViews WHERE orderID = ?"
            val deleteViewsStmt = connection.prepareStatement(deleteViewsQuery)
            deleteViewsStmt.setInt(1, orderId)
            deleteViewsStmt.executeUpdate()
            deleteViewsStmt.close()

            connection.commit()
            println("Order updated successfully with ID: $orderId")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to update order: ${e.message}")
            connection?.rollback()
            false
        } finally {
            connection?.close()
        }
    }
    suspend fun fetchEmployees(): List<Employee> = withContext(Dispatchers.IO) {
        val employeesList = mutableListOf<Employee>()
        var connection: java.sql.Connection? = null
        try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            val query = "SELECT employeeID, name, surname, patronymic, phoneNumber, role, canteen FROM employees"
            val preparedStatement = connection.prepareStatement(query)
            val resultSet = preparedStatement.executeQuery()

            while (resultSet.next()) {
                val name = resultSet.getString("name")?.takeIf { it.isNotEmpty() } ?: ""
                val surname = resultSet.getString("surname")?.takeIf { it.isNotEmpty() } ?: ""
                val patronymic = resultSet.getString("patronymic")?.takeIf { it.isNotEmpty() } ?: ""
                val phoneNumber = resultSet.getString("phoneNumber")?.takeIf { it.isNotEmpty() } ?: ""
                val role = resultSet.getInt("role")
                val canteenId = resultSet.getInt("canteen").takeIf { !resultSet.wasNull() } // Nullable
                println("Fetched employee data: name=$name, surname=$surname, patronymic=$patronymic, phoneNumber=$phoneNumber, role=$role, canteenId=$canteenId")
                val employee = Employee(
                    employeeId = resultSet.getInt("employeeID"),
                    name = name,
                    surname = surname,
                    patronymic = patronymic,
                    phoneNumber = phoneNumber,
                    role = role,
                    canteenId = canteenId
                )
                employeesList.add(employee)
            }

            resultSet.close()
            preparedStatement.close()
            _employees.value = employeesList
        } catch (e: Exception) {
            println("Failed to fetch employees: ${e.message}")
            e.printStackTrace()
        } finally {
            connection?.close()
        }
        employeesList
    }

    suspend fun addCanteen(address: String): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            println("Adding canteen: address=$address")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString) as Connection

            // Вставка новой столовой с автоинкрементом
            val query = "INSERT INTO canteens (canteenAddress) VALUES (?)"
            val preparedStatement = connection.prepareStatement(query, java.sql.Statement.RETURN_GENERATED_KEYS)
            preparedStatement.setString(1, address)
            val rowsAffected = preparedStatement.executeUpdate()
            val generatedKeys = preparedStatement.generatedKeys
            val newCanteenId = if (generatedKeys.next()) generatedKeys.getInt(1) else -1
            preparedStatement.close()

            if (rowsAffected > 0 && newCanteenId != -1) {
                _canteens.value = _canteens.value + Canteen(newCanteenId, address)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to add canteen: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun updateCanteen(canteenId: Int, address: String): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Updating canteen: canteenId=$canteenId, address=$address")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = "UPDATE canteens SET canteenAddress = ? WHERE canteenID = ?"
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setString(1, address)
            preparedStatement.setInt(2, canteenId)
            val rowsAffected = preparedStatement.executeUpdate()

            preparedStatement.close()
            if (rowsAffected > 0) {
                // Обновляем StateFlow
                _canteens.value = _canteens.value.map {
                    if (it.canteenId == canteenId) it.copy(address = address) else it
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to update canteen: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun addEmployee(
        name: String,
        surname: String,
        patronymic: String,
        phoneNumber: String,
        password: String,
        role: Int,
        canteenId: Int?
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            println("Adding employee: name=$name, surname=$surname, phoneNumber=$phoneNumber, role=$role, canteenId=$canteenId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString) as Connection

            // Генерация соли и хэша пароля
            val salt = HashUtils.generateSalt()
            val hashedPassword = HashUtils.hashPassword(password, salt)
            println("Generated salt: $salt")
            println("Generated hash: $hashedPassword")

            // Вставка нового сотрудника с автоинкрементом
            val query = """
                INSERT INTO employees (name, surname, patronymic, phoneNumber, password, salt, role, canteen)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            val preparedStatement = connection.prepareStatement(query, java.sql.Statement.RETURN_GENERATED_KEYS)
            preparedStatement.setString(1, name)
            preparedStatement.setString(2, surname)
            preparedStatement.setString(3, patronymic)
            preparedStatement.setString(4, phoneNumber)
            preparedStatement.setString(5, hashedPassword)
            preparedStatement.setString(6, salt)
            preparedStatement.setInt(7, role)
            if (canteenId != null) {
                preparedStatement.setInt(8, canteenId)
            } else {
                preparedStatement.setNull(8, java.sql.Types.INTEGER)
            }
            val rowsAffected = preparedStatement.executeUpdate()
            val generatedKeys = preparedStatement.generatedKeys
            val newEmployeeId = if (generatedKeys.next()) generatedKeys.getInt(1) else -1
            preparedStatement.close()

            if (rowsAffected > 0 && newEmployeeId != -1) {
                _employees.value = _employees.value + Employee(
                    employeeId = newEmployeeId,
                    name = name,
                    surname = surname,
                    patronymic = patronymic,
                    phoneNumber = phoneNumber,
                    role = role,
                    canteenId = canteenId
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to add employee: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun updateEmployee(employee: Employee): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Updating employee: employeeId=${employee.employeeId}, name=${employee.name}, surname=${employee.surname}, phoneNumber=${employee.phoneNumber}, role=${employee.role}, canteen=${employee.canteenId}")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = """
                UPDATE employees 
                SET name = ?, surname = ?, patronymic = ?, phoneNumber = ?, role = ?, canteen = ?
                WHERE employeeID = ?
            """.trimIndent()
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setString(1, employee.name)
            preparedStatement.setString(2, employee.surname)
            preparedStatement.setString(3, employee.patronymic)
            preparedStatement.setString(4, employee.phoneNumber)
            preparedStatement.setInt(5, employee.role)
            if (employee.canteenId != null) {
                preparedStatement.setInt(6, employee.canteenId)
            } else {
                preparedStatement.setNull(6, java.sql.Types.INTEGER)
            }
            preparedStatement.setInt(7, employee.employeeId)
            val rowsAffected = preparedStatement.executeUpdate()

            preparedStatement.close()
            if (rowsAffected > 0) {
                _employees.value = _employees.value.map {
                    if (it.employeeId == employee.employeeId) employee else it
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to update employee: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun deleteEmployee(employeeId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Deleting employee: employeeId=$employeeId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = "DELETE FROM employees WHERE employeeID = ?"
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, employeeId)
            val rowsAffected = preparedStatement.executeUpdate()

            preparedStatement.close()
            if (rowsAffected > 0) {
                _employees.value = _employees.value.filter { it.employeeId != employeeId }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to delete employee: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun deleteCanteen(canteenId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Deleting canteen: canteenId=$canteenId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            // Проверка, связаны ли сотрудники с этой столовой
            val checkEmployeesQuery = "SELECT COUNT(*) FROM employees WHERE canteenID = ?"
            val checkStatement = connection.prepareStatement(checkEmployeesQuery)
            checkStatement.setInt(1, canteenId)
            val resultSet = checkStatement.executeQuery()
            resultSet.next()
            val employeeCount = resultSet.getInt(1)
            resultSet.close()
            checkStatement.close()

            if (employeeCount > 0) {
                println("Cannot delete canteen: employees are assigned to canteenId=$canteenId")
                return@withContext false
            }

            // Удаление столовой
            val query = "DELETE FROM canteens WHERE canteenID = ?"
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, canteenId)
            val rowsAffected = preparedStatement.executeUpdate()

            preparedStatement.close()
            if (rowsAffected > 0) {
                _canteens.value = _canteens.value.filter { it.canteenId != canteenId }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to delete canteen: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun deleteOrderStatus(statusId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Deleting order status: statusId=$statusId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            // Проверка, связаны ли заказы с этим статусом
            val checkOrdersQuery = "SELECT COUNT(*) FROM orders WHERE statusID = ?"
            val checkStatement = connection.prepareStatement(checkOrdersQuery)
            checkStatement.setInt(1, statusId)
            val resultSet = checkStatement.executeQuery()
            resultSet.next()
            val orderCount = resultSet.getInt(1)
            resultSet.close()
            checkStatement.close()

            if (orderCount > 0) {
                println("Cannot delete status: orders are assigned to statusId=$statusId")
                return@withContext false
            }

            // Удаление статуса
            val query = "DELETE FROM orderStatuses WHERE statusID = ?"
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, statusId)
            val rowsAffected = preparedStatement.executeUpdate()

            preparedStatement.close()
            if (rowsAffected > 0) {
                _statuses.value = _statuses.value.filter { it.statusId != statusId }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to delete order status: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun addStatus(statusName: String): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            println("Adding status: statusName=$statusName")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString) as Connection

            // Вставка нового статуса с автоинкрементом
            val query = "INSERT INTO orderStatus (statusName) VALUES (?)"
            val preparedStatement = connection.prepareStatement(query, java.sql.Statement.RETURN_GENERATED_KEYS)
            preparedStatement.setString(1, statusName)
            val rowsAffected = preparedStatement.executeUpdate()
            val generatedKeys = preparedStatement.generatedKeys
            val newStatusId = if (generatedKeys.next()) generatedKeys.getInt(1) else -1
            preparedStatement.close()

            if (rowsAffected > 0 && newStatusId != -1) {
                _statuses.value = _statuses.value + OrderStatus(newStatusId, statusName)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to add status: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun updateStatus(statusId: Int, statusName: String): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Updating status: statusId=$statusId, statusName=$statusName")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = "UPDATE orderStatus SET statusName = ? WHERE statusID = ?"
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setString(1, statusName)
            preparedStatement.setInt(2, statusId)
            val rowsAffected = preparedStatement.executeUpdate()

            preparedStatement.close()
            if (rowsAffected > 0) {
                // Обновляем StateFlow
                _statuses.value = _statuses.value.map {
                    if (it.statusId == statusId) it.copy(statusName = statusName) else it
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to update status: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    // Получение фактических количеств для заказа
    suspend fun fetchActualQuantities(orderId: Int): Map<Int, Double> = withContext(Dispatchers.IO) {
        val quantities = mutableMapOf<Int, Double>()
        var connection: java.sql.Connection? = null
        try {
            println("Fetching actual quantities for orderId: $orderId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = """
                SELECT productID, actualQuantity 
                FROM actualSentQuantities 
                WHERE orderID = ?
            """.trimIndent()

            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, orderId)
            val resultSet = preparedStatement.executeQuery()

            while (resultSet.next()) {
                val productId = resultSet.getInt("productID")
                val actualQuantity = resultSet.getDouble("actualQuantity")
                quantities[productId] = actualQuantity
            }

            resultSet.close()
            preparedStatement.close()
        } catch (e: Exception) {
            println("Failed to fetch actual quantities: ${e.message}")
            e.printStackTrace()
        } finally {
            connection?.close()
        }
        quantities
    }

    // Обновление фактического количества
    suspend fun updateActualQuantity(
        orderId: Int,
        productId: Int,
        actualQuantity: Double,
        employeeId: Int
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            println("Updating actual quantity: orderId=$orderId, productId=$productId, actualQuantity=$actualQuantity, employeeId=$employeeId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = """
                INSERT INTO actualSentQuantities (orderID, productID, actualQuantity, sentDate, sentByEmployeeID)
                VALUES (?, ?, ?, NOW(), ?)
                ON DUPLICATE KEY UPDATE 
                    actualQuantity = ?,
                    sentDate = NOW(),
                    sentByEmployeeID = ?
            """.trimIndent()

            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, orderId)
            preparedStatement.setInt(2, productId)
            preparedStatement.setDouble(3, actualQuantity)
            preparedStatement.setInt(4, employeeId)
            preparedStatement.setDouble(5, actualQuantity)
            preparedStatement.setInt(6, employeeId)

            val rowsAffected = preparedStatement.executeUpdate()
            preparedStatement.close()

            rowsAffected > 0
        } catch (e: Exception) {
            println("Failed to update actual quantity: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }

    suspend fun getActualQuantity(
        orderId: Int,
        productId: Int
    ): Double? = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        var resultSet: java.sql.ResultSet? = null
        var preparedStatement: java.sql.PreparedStatement? = null
        try {
            println("Fetching actual quantity: orderId=$orderId, productId=$productId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = """
            SELECT actualQuantity
            FROM actualSentQuantities
            WHERE orderID = ? AND productID = ?
        """.trimIndent()

            preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, orderId)
            preparedStatement.setInt(2, productId)

            resultSet = preparedStatement.executeQuery()
            if (resultSet.next()) {
                resultSet.getDouble("actualQuantity")
            } else {
                null
            }
        } catch (e: Exception) {
            println("Failed to fetch actual quantity: ${e.message}")
            e.printStackTrace()
            null
        } finally {
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
        }
    }

    suspend fun getIsAccepted(orderId: Int, productId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        var resultSet: java.sql.ResultSet? = null
        var preparedStatement: java.sql.PreparedStatement? = null
        try {
            println("Fetching isAccepted: orderId=$orderId, productId=$productId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)

            val query = """
            SELECT isAccepted
            FROM actualSentQuantities
            WHERE orderID = ? AND productID = ?
        """.trimIndent()

            preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, orderId)
            preparedStatement.setInt(2, productId)

            resultSet = preparedStatement.executeQuery()
            if (resultSet.next()) {
                resultSet.getBoolean("isAccepted")
            } else {
                false // Если записи нет, считаем, что не принято
            }
        } catch (e: Exception) {
            println("Failed to fetch isAccepted: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            resultSet?.close()
            preparedStatement?.close()
            connection?.close()
        }
    }

    suspend fun deleteActualQuantity(orderId: Int, productId: Int): Boolean = withContext(Dispatchers.IO) {
        var connection: java.sql.Connection? = null
        try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString)
            val query = "DELETE FROM actualSentQuantities WHERE orderID = ? AND productID = ?"
            val stmt = connection.prepareStatement(query)
            stmt.setInt(1, orderId)
            stmt.setInt(2, productId)
            val rows = stmt.executeUpdate()
            stmt.close()
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            connection?.close()
        }
    }
}

data class LoginUiState(
    val phoneNumber: String = "",
    val password: String = ""
)

