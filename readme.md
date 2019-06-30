
# 运行环境
* jdk1.7 +
* tomcat7 +
* maven
* lombok


# pom需要配置servlet3与简化get、set的jar
```
<dependency>
	<groupId>javax.servlet</groupId>
	<artifactId>javax.servlet-api</artifactId>
	<version>3.1.0</version>
	<scope>provided</scope>
</dependency>

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.8</version>
    <scope>provided</scope>
</dependency>
```

# 调度启动类
```
<servlet> 
	<servlet-name>mvc</servlet-name>
	<servlet-class>com.flong.mvc.core.web.servlet.DispatcherServlet</servlet-class>
	<init-param> 
		<param-name>contextConfigLocation</param-name>
		<param-value>application.properties</param-value> 
	</init-param>
	<load-on-startup>1</load-on-startup> 
</servlet> 
<servlet-mapping>
 	<servlet-name>mvc</servlet-name> 
 	<url-pattern>/*</url-pattern>
</servlet-mapping>
 
```

* 上面web.xml配置等同Servlet3的注解
```
@WebServlet(name = "mvc", urlPatterns = { "/*" }, 
initParams = { @WebInitParam(name = "contextConfigLocation", value = "application.properties") },
loadOnStartup = 1)
```

