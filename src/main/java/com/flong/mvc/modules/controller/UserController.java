package com.flong.mvc.modules.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.flong.mvc.core.annotation.beans.Autowired;
import com.flong.mvc.core.annotation.bind.Controller;
import com.flong.mvc.core.annotation.bind.RequestMapping;
import com.flong.mvc.core.annotation.bind.RequestParam;
import com.flong.mvc.modules.service.IUserService;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired("UserServiceImpl")
	IUserService userService;

	@RequestMapping("/test1")
	public void test1(HttpServletRequest request, HttpServletResponse response
			, @RequestParam("userName") String userName) {
		System.out.println(userName);
		try {
			response.getWriter().write("request method success! userName:" + userName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@RequestMapping("/test2")
	public void test2(HttpServletRequest request, HttpServletResponse response) {
		try {
			response.getWriter().println("request method success!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@RequestMapping("/test3")
	public void test3(HttpServletRequest request, HttpServletResponse response
			, @RequestParam("userName") String userName) {
		try {
			
			String userName2 = userService.getUserName(userName);
			
			response.getWriter().println(" hello " + userName2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
