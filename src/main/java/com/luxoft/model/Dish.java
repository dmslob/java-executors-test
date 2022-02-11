package com.luxoft.model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Dish {
	private String name;
	private volatile boolean isWashed;

	public Dish(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isWashed() {
		return isWashed;
	}

	public void setWashed(boolean washed) {
		log.info("setWashed");
		isWashed = washed;
	}
}
