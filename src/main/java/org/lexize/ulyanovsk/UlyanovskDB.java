package org.lexize.ulyanovsk;

import org.lexize.ulyanovsk.annotations.SQLAutoincrement;
import org.lexize.ulyanovsk.annotations.SQLPrimaryKey;
import org.lexize.ulyanovsk.annotations.SQLValue;
import org.lexize.ulyanovsk.models.JailData;
import org.lexize.ulyanovsk.models.DataElement;
import org.lexize.ulyanovsk.models.JailedPlayerSavedData;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;

public class UlyanovskDB {
    private Connection _dbConnection;

    private static final String _defaultSQLType = "TEXT";
    private static final Map<Class<?>, String> _classToSQLType = new HashMap<>() {{
        put(boolean.class, "INTEGER");
        put(byte.class, "INTEGER");
        put(short.class, "INTEGER");
        put(int.class, "INTEGER");
        put(Integer.class, "INTEGER");
        put(long.class, "NUMBER");
        put(Long.class, "NUMBER");
        put(float.class, "NUMBER");
        put(Float.class, "NUMBER");
        put(double.class, "NUMBER");
        put(Double.class, "NUMBER");
        put(String.class, "TEXT");
    }};

    private static final Map<Class<?>, String> _classToSQLTypeGetter = new HashMap<>() {{
        put(boolean.class, "getBoolean");
        put(byte.class, "getByte");
        put(short.class, "getShort");
        put(int.class, "getInt");
        put(Integer.class, "getInt");
        put(long.class, "getLong");
        put(Long.class, "getLong");
        put(float.class, "getFloat");
        put(Float.class, "getFloat");
        put(double.class, "getDouble");
        put(Double.class, "getDouble");
        put(String.class, "getString");
    }};

    public UlyanovskDB(File dbFile) {
        try {
            _dbConnection = DriverManager.getConnection("jdbc:sqlite:%s".formatted(dbFile.toPath().toString()));
            _Init();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void _Init() throws SQLException {
        Statement statement = _dbConnection.createStatement();
        CreateTableByClass(JailData.class, "jailed_players");
        statement.execute("CREATE TABLE IF NOT EXISTS jail_history(element_id INTEGER PRIMARY KEY AUTOINCREMENT, element_type TEXT, element_data TEXT)");
        statement.execute("CREATE TABLE IF NOT EXISTS players_to_unjail(player_uuid TEXT PRIMARY KEY, player_saved_data TEXT)");
        statement.close();
    }
    public boolean IsPlayerInJail(String uuid)  {
        try {
            PreparedStatement statement = _dbConnection.prepareStatement("SELECT count(player_id) = 1 FROM jailed_players WHERE player_id = ?");
            statement.setString(1, uuid);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                boolean val = result.getBoolean(1);
                result.close();
                return val;
            }
            result.close();
        }
        catch (Exception ignored) {}
        return false;
    }
    public void AddPlayerToJail(JailData data) throws SQLException {
        int caseId = InsertObjectIntoTable("jailed_players", data, new String[]{"case_id"}, true);
        data.setCaseID(caseId);
        AddRecordToHistory(data);
    }
    public void AddRecordToHistory(DataElement element) throws SQLException {
        String typeName = element.getClass().getName();
        String elementData = Ulyanovsk.getInstance().getJson().toJson(element);
        PreparedStatement statement = _dbConnection.prepareStatement("INSERT INTO jail_history(element_type,element_data) VALUES(?,?)");
        statement.setString(1,typeName);
        statement.setString(2,elementData);
        statement.execute();
        statement.close();
    }
    public DataElement GetRecordFromHistory(int recordID) throws SQLException, ClassNotFoundException {
        PreparedStatement statement = _dbConnection.prepareStatement("SELECT element_type, element_data FROM jail_history WHERE element_id = ?");
        statement.setInt(1, recordID);
        var result = statement.executeQuery();
        DataElement element = null;
        if (result.next()) {
            Class elementClass = Class.forName(result.getString("element_type"));
            element = (DataElement) Ulyanovsk.getInstance().getJson().fromJson(result.getString("element_data"), elementClass);
        }
        result.close();
        statement.close();
        return element;
    }

    public Map<Integer, DataElement> GetRecordsFromHistory(int limit, int offset) throws SQLException, ClassNotFoundException {
        PreparedStatement statement =
                _dbConnection.prepareStatement(
                        "SELECT element_id, element_type, element_data FROM jail_history ORDER BY element_id DESC LIMIT ? OFFSET ?"
                );
        statement.setInt(1, limit);
        statement.setInt(2, offset);
        Map<Integer, DataElement> elements = new HashMap<>();
        var result = statement.executeQuery();
        while (result.next()) {
            int elementId = result.getInt("element_id");
            Class elementClass = Class.forName(result.getString("element_type"));
            DataElement element = (DataElement) Ulyanovsk.getInstance().getJson().fromJson(result.getString("element_data"), elementClass);
            elements.put(elementId, element);
        }
        result.close();
        statement.close();
        return elements;
    }

    public int GetHistoryRecordsCount() throws SQLException {
        Statement statement = _dbConnection.createStatement();
        var result = statement.executeQuery("SELECT count(*) FROM jail_history");
        result.next();
        int count = result.getInt(1);
        result.close();
        statement.close();
        return count;
    }

    public void AddToUnjailQueue(JailData data) throws SQLException {
        PreparedStatement statement = _dbConnection.prepareStatement("INSERT OR IGNORE INTO players_to_unjail" +
                "(player_uuid, player_saved_data) VALUES(?,?)");
        statement.setString(1, data.getJailedPlayerUUID());
        statement.setString(2, data.getSavedPlayerData().toString());
        statement.execute();
        statement.close();
    }
    public void RemoveJailRecord(JailData data) throws SQLException {
        PreparedStatement statement = _dbConnection.prepareStatement("DELETE FROM jailed_players WHERE case_id = ?");
        statement.setInt(1,data.getCaseID());
        statement.execute();
        statement.close();
    }
    public boolean IsInUnjailQueue(UUID player_uuid) {
        try {
            PreparedStatement statement = _dbConnection.prepareStatement("SELECT count(player_uuid) = 1 FROM players_to_unjail WHERE player_uuid = ?");
            statement.setString(1, player_uuid.toString());
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                boolean val = result.getBoolean(1);
                result.close();
                return val;
            }
            result.close();
        }
        catch (Exception ignored) {}
        return false;
    }
    public JailedPlayerSavedData GetSavedDataFromUnjailQueue(UUID player_uuid) throws SQLException {
        PreparedStatement statement = _dbConnection.prepareStatement("SELECT player_saved_data FROM players_to_unjail WHERE player_uuid = ?");
        statement.setString(1, player_uuid.toString());
        var result = statement.executeQuery();
        JailedPlayerSavedData dataResult = null;
        if (result.next()) {
            String data = result.getString("player_saved_data");
            dataResult = Ulyanovsk.getInstance().getJson().fromJson(data, JailedPlayerSavedData.class);
        }
        result.close();
        statement.close();
        return dataResult;
    }
    public void RemoveFromUnjailQueue(UUID player_uuid) throws SQLException {
        PreparedStatement statement = _dbConnection.prepareStatement("DELETE FROM players_to_unjail WHERE player_uuid = ?");
        statement.setString(1, player_uuid.toString());
        statement.execute();
        statement.close();
    }
    public List<JailData> GetJailData() throws SQLException {
        Statement statement = _dbConnection.createStatement();
        var resultSet = statement.executeQuery("SELECT * FROM jailed_players");
        List<JailData> data = new ArrayList<>();
        while (resultSet.next()) {
            data.add(GetObjectFromResultSet(JailData.class, resultSet));
        }
        resultSet.close();
        statement.close();
        return data;
    }
    public List<JailData> GetJailDataToRelease(long time) throws SQLException {
        Statement statement = _dbConnection.createStatement();
        var resultSet = statement.executeQuery("SELECT * FROM jailed_players WHERE jail_length <> -1 AND jail_start_time + jail_length < %s".formatted(time));
        List<JailData> data = new ArrayList<>();
        while (resultSet.next()) {
            data.add(GetObjectFromResultSet(JailData.class, resultSet));
        }
        resultSet.close();
        statement.close();
        return data;
    }
    public void UpdatePlayerJailTime(String uuid, long jail_until) throws SQLException {
        PreparedStatement statement = _dbConnection.prepareStatement("UPDATE jailed_players SET jail_until = ? WHERE player_id = ?");
        statement.setLong(1,jail_until);
        statement.setString(2,uuid);
        statement.execute();
        statement.close();
    }
    public void UpdatePlayerJailReason(String uuid, String reason) throws SQLException {
        PreparedStatement statement = _dbConnection.prepareStatement("UPDATE jailed_players SET reason = ? WHERE player_id = ?");
        statement.setString(1,reason);
        statement.setString(2,uuid);
        statement.execute();
        statement.close();
    }
    public JailData GetJailData(int case_id) throws SQLException {
        PreparedStatement statement = _dbConnection.prepareStatement("SELECT * FROM jailed_players WHERE case_id = ?");
        statement.setLong(1, case_id);
        ResultSet result = statement.executeQuery();
        JailData data = null;
        if (result.next()) {
            data = GetObjectFromResultSet(JailData.class, result);
        }
        result.close();
        statement.close();
        return data;
    }

    private void CreateTableByClass(Class<?> sourceClass, String tableName) throws SQLException {
        List<Field> fields = Ulyanovsk.Utils.GetAllFields(sourceClass);
        Map<String, String> typeMap = new HashMap<>();
        for (Field field :
                fields) {
            if (!field.isAnnotationPresent(SQLValue.class)) continue;
            String valueName = field.getAnnotation(SQLValue.class).value();
            if (typeMap.containsValue(valueName)) continue;
            List<String> columnSignature = new ArrayList<>();
            columnSignature.add(valueName);
            columnSignature.add(_classToSQLType.getOrDefault(field.getType(), _defaultSQLType));
            if (field.isAnnotationPresent(SQLPrimaryKey.class)) columnSignature.add("PRIMARY KEY");
            if (field.isAnnotationPresent(SQLAutoincrement.class)) columnSignature.add("AUTOINCREMENT");
            typeMap.put(valueName, String.join(" ", columnSignature));
        }
        String columns = String.join(", ", typeMap.values());
        String creationStatement = "CREATE TABLE IF NOT EXISTS %s(%s)".formatted(tableName, columns);
        var statement = _dbConnection.createStatement();
        statement.execute(creationStatement);
        statement.close();
    }
    private int InsertObjectIntoTable(String tableName, Object o, String[] includeFields, boolean exclude) throws SQLException {
        Class<?> sourceClass = o.getClass();
        List<Field> fields = Ulyanovsk.Utils.GetAllFields(sourceClass);
        Map<String, Object> data = new HashMap<>();
        for (Field field :
                fields) {
            if (!field.isAnnotationPresent(SQLValue.class)) continue;
            String valueName = field.getAnnotation(SQLValue.class).value();
            field.setAccessible(true);
            try {
                if (_classToSQLType.containsKey(field.getType())) data.put(valueName, field.get(o));
                else {
                    Object fO = field.get(o);
                    String serializedFO = Ulyanovsk.getInstance().getJson().toJson(fO);
                    data.put(valueName, serializedFO);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        String[] empty = new String[0];
        List<String> headerData = new ArrayList<>() {{
            for (String k :
                    data.keySet()) {
                boolean match = Arrays.asList(includeFields != null ? includeFields : empty).contains(k);
                if ((match & !exclude) || (exclude & !match)) {
                    add(k);
                }
            }
        }};
        String headerString = String.join(",", headerData);
        String valueString = String.join(",", new ArrayList<String>() {{
            for (String s:
                 headerData) {
                add("?");
            }
        }});
        String statementString = "INSERT INTO %s(%s) VALUES(%s);"
                .formatted(tableName, headerString, valueString);
        PreparedStatement statement = _dbConnection.prepareStatement(statementString);
        for (int i = 0; i < headerData.size(); i++) {
            String key = headerData.get(i);
            statement.setObject(i+1, data.get(key));
        }
        statement.execute();
        statement.close();
        var rstmt = _dbConnection.createStatement();
        var result = rstmt.executeQuery("SELECT last_insert_rowid();");
        result.next();
        int rowid = result.getInt(1);
        result.close();
        rstmt.close();
        return rowid;
    }
    private <T> T GetObjectFromResultSet(Class<T> objectClass, ResultSet result) {
        try {
            Class<ResultSet> resultSetClass = ResultSet.class;
            var constructor = objectClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T obj = constructor.newInstance();
            Map<String, Field> fieldReferences = new HashMap<>();
            for (Field field :
                    Ulyanovsk.Utils.GetAllFields(objectClass)) {
                if (!field.isAnnotationPresent(SQLValue.class)) continue;
                String valueName = field.getAnnotation(SQLValue.class).value();
                fieldReferences.put(valueName, field);
            }
            for (Map.Entry<String, Field> sf :
                    fieldReferences.entrySet()) {
                String key = sf.getKey();
                Field field = sf.getValue();
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                if (_classToSQLTypeGetter.containsKey(fieldType)) {
                    Method m = resultSetClass.getMethod(_classToSQLTypeGetter.get(fieldType), String.class);
                    Object o = m.invoke(result, key);
                    field.set(obj, o);
                }
                else {
                    String serializedData = result.getString(key);
                    Object o = Ulyanovsk.getInstance().getJson().fromJson(serializedData, fieldType);
                    field.set(obj, o);
                }
            }
            return obj;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                 SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() throws SQLException {
        _dbConnection.close();
    }
}
