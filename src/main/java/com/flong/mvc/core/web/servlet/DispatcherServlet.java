package com.flong.mvc.core.web.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.flong.mvc.core.annotation.beans.Autowired;
import com.flong.mvc.core.annotation.beans.Component;
import com.flong.mvc.core.annotation.beans.Qualifier;
import com.flong.mvc.core.annotation.beans.Repository;
import com.flong.mvc.core.annotation.beans.Service;
import com.flong.mvc.core.annotation.bind.Controller;
import com.flong.mvc.core.annotation.bind.RequestMapping;

@WebServlet(name = "mvc", urlPatterns = { "/*" }, initParams = {
		@WebInitParam(name = "contextConfigLocation", value = "application.properties") }, loadOnStartup = 1)
public class DispatcherServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * Properties
	 */
	private Properties properties = new Properties();

	/**
	 * 类名Map
	 */
	private List<String> classNames = new ArrayList<String>();

	/**
	 * ioc容器Map
	 */
	private Map<String, Object> ioc = new HashMap<String, Object>();

	/**
	 * 句柄容器Map
	 */
	private Map<String, Method> handlerMapping = new HashMap<String, Method>();

	/**
	 * 控制层容器Map
	 */
	private Map<String, Object> controllerMap = new HashMap<String, Object>();

	public DispatcherServlet() {
		super();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// 1.加载配置文件
		doLoadConfig(config.getInitParameter("contextConfigLocation"));

		// 2.初始化所有相关联的类,扫描用户设定的包下面所有的类
		doScanner(properties.getProperty("scanPackage"));

		// 3.拿到扫描到的类,通过反射机制,实例化,并且放到ioc容器中(k-v-->beanName-bean) beanName默认是首字母小写
		doInstance();

		// 4、自动配置
		doAutowired();
		// 5.初始化HandlerMapping(将url和method对应上)
		initHandlerMapping();

	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		try {
			// 处理请求
			doDispatch(request, response);
		} catch (Exception e) {
			response.getWriter().write("500!! Server Exception");
		}
	}

	/**
	 * 
	 * @Description 调度方法
	 * @Author liangjl
	 * @Date 2019年6月30日 下午4:13:51
	 * @param request
	 * @param response
	 * @throws Exception 参数
	 * @return void 返回类型
	 */
	private void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// 请求URI连接
		String url = request.getRequestURI();

		// 请求上下文件路径
		String contextPath = request.getContextPath();

		url = url.replace(contextPath, "").replaceAll("/+", "/");

		if (!this.handlerMapping.containsKey(url)) {
			response.getWriter().write("404 NOT FOUND!");
			return;
		}

		Method method = this.handlerMapping.get(url);

		// 获取方法的参数列表
		Class<?>[] parameterTypes = method.getParameterTypes();

		// 获取请求的参数
		Map<String, String[]> parameterMap = request.getParameterMap();

		// 保存参数值
		Object[] paramValues = new Object[parameterTypes.length];
		// 方法的参数列表
		for (int i = 0; i < parameterTypes.length; i++) {
			// 根据参数名称，做某些处理
			String requestParam = parameterTypes[i].getSimpleName();

			if (requestParam.equals("HttpServletRequest")) {
				// 参数类型已明确，这边强转类型
				paramValues[i] = request;
				continue;
			}
			if (requestParam.equals("HttpServletResponse")) {
				paramValues[i] = response;
				continue;
			}
			if (requestParam.equals("String")) {
				for (Entry<String, String[]> param : parameterMap.entrySet()) {
					String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
					paramValues[i] = value;
				}
			}
		}
		// 利用反射机制来调用
		try {
			// 第一个参数是method所对应的实例 在ioc容器中
			method.invoke(this.controllerMap.get(url), paramValues);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 扫描配置
	private void doLoadConfig(String location) {
		// 把web.xml中的contextConfigLocation对应value值的文件加载到流里面
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
		try {
			// 用Properties文件加载文件里的内容
			properties.load(resourceAsStream);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 关流
			if (null != resourceAsStream) {
				try {
					resourceAsStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	// 扫描包
	private void doScanner(String packageName) {
		// 把所有的.替换成/
		URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
		File dir = new File(url.getFile());
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				// 递归读取包
				doScanner(packageName + "." + file.getName());
			} else {
				String className = packageName + "." + file.getName().replace(".class", "");
				classNames.add(className);
			}
		}
	}

	/**
	 * @Description 处理所有类的组件实例
	 * @Author liangjl
	 * @Date 2019年6月30日 下午4:12:46
	 */
	private void doInstance() {
		if (classNames.isEmpty()) {
			return;
		}
		for (String className : classNames) {
			try {
				// 把类搞出来,反射来实例化(只有加@MyController需要实例化)
				Class<?> clazz = Class.forName(className);
				if (clazz.isAnnotationPresent(Controller.class)) {
					ioc.put(toLowerFirstWord(clazz.getSimpleName()), clazz.newInstance());
				} else if (clazz.isAnnotationPresent(Service.class)) {
					// 通过Service获取值，作为beans的key
					Service service = clazz.getAnnotation(Service.class);

					String beanName = service.value();
					// 1.默认采用类名首字母小写 beanId
					// 2.如果自定义名字,优先使用自己定义的名字
					if ("".equals(beanName.trim())) {
						beanName = toLowerFirstWord(clazz.getSimpleName());
					}

					Object instance = clazz.newInstance();
					ioc.put(beanName, instance);
					// 3.根据类型匹配,利用实现类的接口名字作为key,实现类的类做为value
					Class<?>[] interfaces = clazz.getInterfaces();
					for (Class<?> i : interfaces) {
						ioc.put(toLowerFirstWord(i.getSimpleName()), instance);
					}
				} else if (clazz.isAnnotationPresent(Component.class)) {
					// 通过Component获取值，作为beans的key
					Component component = clazz.getAnnotation(Component.class);
					String beanName = component.value();
					// 1.默认采用类名首字母小写 beanId
					// 2.如果自定义名字,优先使用自己定义的名字
					if ("".equals(beanName.trim())) {
						beanName = toLowerFirstWord(clazz.getSimpleName());
					}

					Object instance = clazz.newInstance();
					ioc.put(beanName, instance);
					// 3.根据类型匹配,利用实现类的接口名字作为key,实现类的类做为value
					Class<?>[] interfaces = clazz.getInterfaces();
					for (Class<?> i : interfaces) {
						ioc.put(toLowerFirstWord(i.getSimpleName()), instance);
					}

				} else if (clazz.isAnnotationPresent(Repository.class)) {
					// 通过Repository获取值，作为beans的key
					Repository repository = clazz.getAnnotation(Repository.class);
					String beanName = repository.value();
					// 1.默认采用类名首字母小写 beanId
					// 2.如果自定义名字,优先使用自己定义的名字
					if ("".equals(beanName.trim())) {
						beanName = toLowerFirstWord(clazz.getSimpleName());
					}

					Object instance = clazz.newInstance();
					ioc.put(beanName, instance);
					// 3.根据类型匹配,利用实现类的接口名字作为key,实现类的类做为value
					Class<?>[] interfaces = clazz.getInterfaces();
					for (Class<?> i : interfaces) {
						ioc.put(toLowerFirstWord(i.getSimpleName()), instance);
					}

				} else {
					continue;
				}

			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}

	/**
	 * @Description 处理RequestMapping
	 * @Author liangjl
	 * @Date 2019年6月30日 下午4:12:26
	 * @return void 返回类型
	 */
	private void initHandlerMapping() {
		if (ioc.isEmpty()) {
			return;
		}
		try {
			for (Entry<String, Object> entry : ioc.entrySet()) {
				Class<? extends Object> clazz = entry.getValue().getClass();
				if (!clazz.isAnnotationPresent(Controller.class)) {
					continue;
				}

				// 拼url时,是controller头的url拼上方法上的url
				String baseUrls = "";
				if (clazz.isAnnotationPresent(RequestMapping.class)) {
					RequestMapping annotation = clazz.getAnnotation(RequestMapping.class);
					baseUrls = annotation.value();
				}
				Method[] methods = clazz.getMethods();
				for (Method method : methods) {
					if (!method.isAnnotationPresent(RequestMapping.class)) {
						continue;
					}

					RequestMapping annotation = method.getAnnotation(RequestMapping.class);
					String urls = annotation.value();
					String url = (baseUrls + "/" + urls).replaceAll("/+", "/");
					handlerMapping.put(url, method);
					controllerMap.put(url, clazz.newInstance());
				}

				Object instance = entry.getValue();
				Field[] fields = clazz.getDeclaredFields();
				for (Field field : fields) {
					// 如果当前的成员变量使用注解CustomRequestMapping进行处理
					if (field.isAnnotationPresent(Qualifier.class)) {
						// 获取当前成员变量的注解值
						Qualifier qualifier = field.getAnnotation(Qualifier.class);
						String value = qualifier.value();

						// 由于此类成员变量设置为private，需要强行设置
						field.setAccessible(true);
						// 将beans的实例化对象赋值给当前的变量
						try {
							field.set(instance, ioc.get(value));
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					} else {
						continue;
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void doAutowired() {
		if (ioc.isEmpty()) {
			return;
		}
		for (Map.Entry<String, Object> entry : ioc.entrySet()) {

			// 在Spring中,没有隐私
			Field[] fields = entry.getValue().getClass().getDeclaredFields();

			Object instnce = entry.getValue();
			Class<?> clazz = instnce.getClass();

			if (clazz.isAnnotationPresent(Controller.class)) {
				for (Field field : fields) {
					// 找autowired
					if (!field.isAnnotationPresent(Autowired.class)) {
						continue;
					}

					Autowired autowired = field.getAnnotation(Autowired.class);
					String beanName = autowired.value().trim();

					if ("".equals(beanName)) {
						beanName = toLowerFirstWord(field.getType().getSimpleName());
					}
					// 暴力反射
					field.setAccessible(true);
					try {
						field.set(entry.getValue(), ioc.get(beanName));
						System.out.println(entry.getValue() + " is autowired ,object is " + ioc.get(beanName));
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
				}

			} else {
				continue;
			}

		}
	}

	/**
	 * 把字符串的首字母小写
	 * 
	 * @param name
	 * @return
	 */
	private String toLowerFirstWord(String name) {
		char[] charArray = name.toCharArray();
		charArray[0] += 32;
		return String.valueOf(charArray);
	}

}
