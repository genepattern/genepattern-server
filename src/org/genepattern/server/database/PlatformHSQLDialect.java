package org.genepattern.server.database;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.exception.ViolatedConstraintNameExtracter;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.JoinFragment;

/**
 * Added this class for debugging purposes.
 */
public class PlatformHSQLDialect extends HSQLDialect {

    @Override
    public String appendIdentitySelectToInsert(String insertString) {
        String rval=super.appendIdentitySelectToInsert(insertString);
        return rval;
    }

    @Override
    public String appendLockHint(LockMode mode, String tableName) {
        String rval=super.appendLockHint(mode, tableName);
        return rval;
    }

    @Override
    public boolean bindLimitParametersInReverseOrder() {
        boolean rval=super.bindLimitParametersInReverseOrder();
        return rval;
    }

    @Override
    public SQLExceptionConverter buildSQLExceptionConverter() {
        return super.buildSQLExceptionConverter();
    }

    @Override
    public char openQuote() {
        char rval=super.openQuote();
        return rval;
        //return '\0';
    }

    @Override
    public char closeQuote() {
        char rval=super.closeQuote();
        return rval;
        //return '\0';
    }

    @Override
    public CaseFragment createCaseFragment() {
        CaseFragment rval=super.createCaseFragment();
        return rval;
    }

    @Override
    public JoinFragment createOuterJoinFragment() {
        JoinFragment rval=super.createOuterJoinFragment();
        return rval;
    }

    @Override
    public boolean dropConstraints() {
        boolean rval=super.dropConstraints();
        return rval;
    }

    @Override
    public boolean dropTemporaryTableAfterUse() {
        boolean rval=super.dropTemporaryTableAfterUse();
        return rval;
    }

    @Override
    public boolean forUpdateOfColumns() {
        boolean rval=super.forUpdateOfColumns();
        return rval;
    }

    @Override
    public String generateTemporaryTableName(String baseTableName) {
        String rval=super.generateTemporaryTableName(baseTableName);
        return rval;
    }

    @Override
    public String getAddForeignKeyConstraintString(String constraintName, String[] foreignKey, String referencedTable, String[] primaryKey, boolean referencesPrimaryKey) {
        String rval=super.getAddForeignKeyConstraintString(constraintName, foreignKey, referencedTable, primaryKey, referencesPrimaryKey);
        return rval;
    }

    @Override
    public String getAddPrimaryKeyConstraintString(String constraintName) {
        String rval=super.getAddPrimaryKeyConstraintString(constraintName);
        return rval;
    }

    @Override
    public String getCascadeConstraintsString() {
        String rval=super.getCascadeConstraintsString();
        return rval;
    }

    @Override
    public String getCastTypeName(int code) {
        String rval=super.getCastTypeName(code);
        return rval;
    }

    @Override
    public String getColumnComment(String comment) {
        String rval=super.getColumnComment(comment);
        return rval;
    }

    @Override
    protected String getCreateSequenceString(String sequenceName) throws MappingException {
        String rval=super.getCreateSequenceString(sequenceName);
        return rval;
    }

    @Override
    public String getCreateTemporaryTablePostfix() {
        String rval=super.getCreateTemporaryTablePostfix();
        return rval;
    }

    @Override
    public String getCreateTemporaryTableString() {
        String rval=super.getCreateTemporaryTableString();
        return rval;
    }

    @Override
    public String getCurrentTimestampSQLFunctionName() {
        String rval=super.getCurrentTimestampSQLFunctionName();
        return rval;
    }

    @Override
    public String getCurrentTimestampSelectString() {
        String rval=super.getCurrentTimestampSelectString();
        return rval;
    }

    @Override
    public String getDropForeignKeyString() {
        String rval=super.getDropForeignKeyString();
        return rval;
    }

    @Override
    protected String getDropSequenceString(String sequenceName) throws MappingException {
        String rval=super.getDropSequenceString(sequenceName);
        return rval;
    }

    @Override
    public String getForUpdateNowaitString() {
        String rval=super.getForUpdateNowaitString();
        return rval;
    }

    @Override
    public String getForUpdateNowaitString(String aliases) {
        String rval=super.getForUpdateNowaitString(aliases);
        return rval;
    }

    @Override
    public String getForUpdateString(LockMode lockMode) {
        String rval=super.getForUpdateString(lockMode);
        return rval;
    }

    @Override
    public String getForUpdateString(String aliases) {
        String rval=super.getForUpdateString(aliases);
        return rval;
    }

    @Override
    public String getHibernateTypeName(int code, int length, int precision, int scale) throws HibernateException {
        String rval=super.getHibernateTypeName(code, length, precision, scale);
        return rval;
    }

    @Override
    public String getHibernateTypeName(int code) throws HibernateException {
        String rval=super.getHibernateTypeName(code);
        return rval;
    }

    @Override
    public String getIdentityColumnString(int type) throws MappingException {
        String rval=super.getIdentityColumnString(type);
        return rval;
    }

    @Override
    public String getIdentitySelectString(String table, String column, int type) throws MappingException {
        String rval=super.getIdentitySelectString(table, column, type);
        return rval;
    }

    
    @Override
    public Set getKeywords() {
        //Set<String> keywords=new HashSet<String>();
        //keywords.add("key");
        Set rval=super.getKeywords();
        //return keywords;
        return rval;
    }

    @Override
    public String getLimitString(String querySelect, int offset, int limit) {
        String rval=super.getLimitString(querySelect, offset, limit);
        return rval;
    }

    @Override
    public String getLowercaseFunction() {
        String rval=super.getLowercaseFunction();
        return rval;
    }

    @Override
    public int getMaxAliasLength() {
        int rval=super.getMaxAliasLength();
        return rval;
    }

    @Override
    public Class getNativeIdentifierGeneratorClass() {
        Class rval=super.getNativeIdentifierGeneratorClass();
        return rval;
    }

    @Override
    public String getNoColumnsInsertString() {
        String rval=super.getNoColumnsInsertString();
        return rval;
    }

    @Override
    public String getNullColumnString() {
        String rval=super.getNullColumnString();
        return rval;
    }

    @Override
    public ResultSet getResultSet(CallableStatement ps) throws SQLException {
        ResultSet rval=super.getResultSet(ps);
        return rval;
    }

    @Override
    public String getSelectClauseNullString(int sqlType) {
        String rval=super.getSelectClauseNullString(sqlType);
        return rval;
    }

    @Override
    public String getSelectGUIDString() {
        String rval=super.getSelectGUIDString();
        return rval;
    }

    @Override
    public String getTableComment(String comment) {
        String rval=super.getTableComment(comment);
        return rval;
    }

    @Override
    public String getTableTypeString() {
        String rval=super.getTableTypeString();
        return rval;
    }

    @Override
    public String getTypeName(int code, int length, int precision, int scale) throws HibernateException {
        String rval=super.getTypeName(code, length, precision, scale);
        return rval;
    }

    @Override
    public String getTypeName(int code) throws HibernateException {
        String rval=super.getTypeName(code);
        return rval;
    }

    @Override
    public boolean hasAlterTable() {
        boolean rval=super.hasAlterTable();
        return rval;
    }

    @Override
    public boolean hasDataTypeInIdentityColumn() {
        boolean rval=super.hasDataTypeInIdentityColumn();
        return rval;
    }

    @Override
    public boolean hasSelfReferentialForeignKeyBug() {
        boolean rval=super.hasSelfReferentialForeignKeyBug();
        return rval;
    }

    @Override
    public boolean isCurrentTimestampSelectStringCallable() {
        boolean rval=super.isCurrentTimestampSelectStringCallable();
        return rval;
    }

    @Override
    public Boolean performTemporaryTableDDLInIsolation() {
        Boolean rval=super.performTemporaryTableDDLInIsolation();
        return rval;
    }

    @Override
    public boolean qualifyIndexName() {
        boolean rval=super.qualifyIndexName();
        return rval;
    }

    @Override
    protected void registerColumnType(int code, int capacity, String name) {
        super.registerColumnType(code, capacity, name);
    }

    @Override
    protected void registerColumnType(int code, String name) {
        super.registerColumnType(code, name);
    }

    @Override
    protected void registerFunction(String name, SQLFunction function) {
        super.registerFunction(name, function);
    }

    @Override
    protected void registerHibernateType(int sqlcode, int capacity, String name) {
        super.registerHibernateType(sqlcode, capacity, name);
    }

    @Override
    protected void registerHibernateType(int sqlcode, String name) {
        super.registerHibernateType(sqlcode, name);
    }

    @Override
    protected void registerKeyword(String word) {
        super.registerKeyword(word);
    }

    @Override
    public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
        int rval=super.registerResultSetOutParameter(statement, col);
        return rval;
    }

    @Override
    public boolean supportsCascadeDelete() {
        boolean rval=super.supportsCascadeDelete();
        return rval;
    }

    @Override
    public boolean supportsCommentOn() {
        boolean rval=super.supportsCommentOn();
        return rval;
    }

    @Override
    public boolean supportsIfExistsBeforeTableName() {
        boolean rval=super.supportsIfExistsBeforeTableName();
        return rval;
    }

    @Override
    public boolean supportsInsertSelectIdentity() {
        boolean rval=super.supportsInsertSelectIdentity();
        return rval;
    }

    @Override
    public boolean supportsLimitOffset() {
        boolean rval=super.supportsLimitOffset();
        return rval;
    }

    @Override
    public boolean supportsNotNullUnique() {
        boolean rval=super.supportsNotNullUnique();
        return rval;
    }

    @Override
    public boolean supportsOuterJoinForUpdate() {
        boolean rval=super.supportsOuterJoinForUpdate();
        return rval;
    }

    @Override
    public boolean supportsParametersInInsertSelect() {
        boolean rval=super.supportsParametersInInsertSelect();
        return rval;
    }

    @Override
    public boolean supportsTableCheck() {
        boolean rval=super.supportsTableCheck();
        return rval;
    }

    @Override
    public boolean supportsUnionAll() {
        boolean rval=super.supportsUnionAll();
        return rval;
    }

    @Override
    public boolean supportsUniqueConstraintInCreateAlterTable() {
        boolean rval=super.supportsUniqueConstraintInCreateAlterTable();
        return rval;
    }

    @Override
    public boolean supportsVariableLimit() {
        boolean rval=super.supportsVariableLimit();
        return rval;
    }

    @Override
    public String toBooleanValueString(boolean bool) {
        String rval=super.toBooleanValueString(bool);
        return rval;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String transformSelectString(String select) {
        String rval=super.transformSelectString(select);
        return rval;
    }

    @Override
    public boolean useInputStreamToInsertBlob() {
        boolean rval=super.useInputStreamToInsertBlob();
        return rval;
    }

    @Override
    public boolean useMaxForLimit() {
        boolean rval=super.useMaxForLimit();
        return rval;
    }

    @Override
    public boolean bindLimitParametersFirst() {
        boolean rval=bindLimitParametersFirst();
        return rval;
    }

    @Override
    public String getAddColumnString() {
        String str=getAddColumnString();
        return str;
    }

    @Override
    public String[] getCreateSequenceStrings(String sequenceName) {
        String[] rval=super.getCreateSequenceStrings(sequenceName);
        return rval;
    }

    @Override
    public String[] getDropSequenceStrings(String sequenceName) {
        String[] rval=super.getDropSequenceStrings(sequenceName);
        return rval;
    }

    @Override
    public String getForUpdateString() {
        String rval=super.getForUpdateString();
        return rval;
    }

    @Override
    public String getIdentityColumnString() {
        String rval=super.getIdentityColumnString();
        return rval;
    }

    @Override
    public String getIdentityInsertString() {
        String rval=super.getIdentityInsertString();
        return rval;
    }

    @Override
    public String getIdentitySelectString() {
        String rval=super.getIdentitySelectString();
        return rval;
    }

    @Override
    public String getLimitString(String sql, boolean hasOffset) {
        String rval=super.getLimitString(sql, hasOffset);
        return rval;
    }

    @Override
    public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
        LockingStrategy rval=super.getLockingStrategy(lockable, lockMode);
        return rval;
    }

    @Override
    public String getQuerySequencesString() {
        String rval=super.getQuerySequencesString();
        return rval;
    }

    @Override
    public String getSelectSequenceNextValString(String sequenceName) {
        String rval=super.getSelectSequenceNextValString(sequenceName);
        return rval;
    }

    @Override
    public String getSequenceNextValString(String sequenceName) {
        String rval=super.getSequenceNextValString(sequenceName);
        return rval;
    }

    @Override
    public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
        ViolatedConstraintNameExtracter rval=super.getViolatedConstraintNameExtracter();
        return rval;
    }

    @Override
    public boolean supportsColumnCheck() {
        boolean rval=super.supportsColumnCheck();
        return rval;
    }

    @Override
    public boolean supportsCurrentTimestampSelection() {
        boolean rval=super.supportsCurrentTimestampSelection();
        return rval;
    }

    @Override
    public boolean supportsIdentityColumns() {
        boolean rval=super.supportsIdentityColumns();
        return rval;
    }

    @Override
    public boolean supportsIfExistsAfterTableName() {
        boolean rval=super.supportsIfExistsAfterTableName();
        return rval;
    }

    @Override
    public boolean supportsLimit() {
        boolean rval=super.supportsLimit();
        return rval;
    }

    @Override
    public boolean supportsSequences() {
        boolean rval=super.supportsSequences();
        return rval;
    }

    @Override
    public boolean supportsTemporaryTables() {
        boolean rval=super.supportsTemporaryTables();
        return rval;
    }

    @Override
    public boolean supportsUnique() {
        boolean rval=super.supportsUnique();
        return rval;
    }
    
}
