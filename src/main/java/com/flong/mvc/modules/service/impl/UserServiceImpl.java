package com.flong.mvc.modules.service.impl;

import com.flong.mvc.core.annotation.beans.Service;
import com.flong.mvc.modules.service.IUserService;

@Service("UserServiceImpl")
public class UserServiceImpl implements IUserService{

	public String getUserName(String userName) {
		return userName;
	}

}
