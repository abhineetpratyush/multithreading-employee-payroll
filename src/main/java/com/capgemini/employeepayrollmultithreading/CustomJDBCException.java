package com.capgemini.employeepayrollmultithreading;

enum ExceptionType{
	SQL_EXCEPTION, RECORD_UPDATE_FAILURE, CLASS_NOT_FOUND, UNABLE_TO_USE_PREPARED_STATEMENT, UNABLE_TO_USE_STATEMENT, RESULT_SET_PROBLEM, UNABLE_TO_ADD_RECORD_TO_DB, UNABLE_TO_ESTABLISH_CONNECTION, UNABLE_TO_CLOSE_CONNECTION, UNABLE_TO_SET_AUTO_COMMIT, UNABLE_TO_ROLLBACK, UNABLE_TO_COMMIT, UNABLE_TO_DELETE_RECORD
}

public class CustomJDBCException extends Exception{
	public CustomJDBCException(ExceptionType exceptionType) {
		super(exceptionType.toString());
	}
}






