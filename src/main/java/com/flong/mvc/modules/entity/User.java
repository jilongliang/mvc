package com.flong.mvc.modules.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class User implements Serializable{
	//用户Id
	private String userId;
	//用户名
	private String userName;
	//用户密码
	private String passWord;
}
