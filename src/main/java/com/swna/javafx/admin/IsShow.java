package com.swna.javafx.admin;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class IsShow {

	String abbr;
	String shop;
	Boolean is;

	@Override
	public String toString() {
		return "IsShow [abbr=" + abbr + ", shop=" + shop + ", is=" + is + "]\n";
	}
}
