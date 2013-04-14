package com.duality.server.xValidation;


public class Query {
	private final String query;
	private final String actualResponse;

	public Query(String query, String actualResponse) {
		super();
		this.query = query;
		this.actualResponse = actualResponse;
	}
	public String getQuery() {
		return query;
	}
	public String getActualResponse() {
		return actualResponse;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}

		if(obj instanceof Query) {
			Query that = (Query) obj;
			return this.query.equals(that.query) && this.actualResponse.equals(that.actualResponse);
		}

		return false;
	}
	
	@Override
	public int hashCode() {
		return query.hashCode() + 31 * actualResponse.hashCode();
	}
}
