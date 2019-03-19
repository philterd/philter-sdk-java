package com.mtnfog.philter.model;

public class FilterResponse {
	
	private String filteredText;
	
	public FilterResponse() {
		
	}

    public FilterResponse(String filteredText) {
        this.filteredText = filteredText;
    }

    public String getFilteredText() {
        return filteredText;
    }

    public void setFilteredText(String filteredText) {
        this.filteredText = filteredText;
    }

}
