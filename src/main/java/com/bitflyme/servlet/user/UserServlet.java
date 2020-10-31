package com.bitflyme.servlet.user;

import com.bitflyme.pojo.Role;
import com.bitflyme.pojo.User;
import com.bitflyme.service.role.RoleService;
import com.bitflyme.service.role.RoleServiceImpl;
import com.bitflyme.service.user.UserService;
import com.bitflyme.service.user.UserServiceImpl;
import com.bitflyme.tools.Constants;
import com.bitflyme.tools.PageSupport;
import com.alibaba.fastjson.JSONArray;
import com.mysql.jdbc.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserServlet extends HttpServlet {

	public UserServlet() {
		super();
	}

	public void destroy() {
		super.destroy();
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		String method = request.getParameter("method");
		
		System.out.println("method----> " + method);
		
		if(method != null && method.equals("add")){
			//增加操作
			this.add(request, response);
		}else if(method != null && method.equals("query")){
			this.query(request, response);
		}else if(method != null && method.equals("getrolelist")){
			this.getRoleList(request, response);
		}else if(method != null && method.equals("ucexist")){
			this.userCodeExist(request, response);
		}else if(method != null && method.equals("deluser")){
			this.delUser(request, response);
		}else if(method != null && method.equals("view")){
			this.getUserById(request, response,"userview.jsp");
		}else if(method != null && method.equals("modify")){
			this.getUserById(request, response,"usermodify.jsp");
		}else if(method != null && method.equals("modifyexe")){
			this.modify(request, response);
		}else if(method != null && method.equals("pwdmodify")){
			this.getPwdByUserId(request, response);
		}else if(method != null && method.equals("savepwd")){
			this.updatePwd(request, response);
		}
		
	}
	//更新原有密码
	private void updatePwd(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		Object o = request.getSession().getAttribute(Constants.USER_SESSION);
		String newpassword = request.getParameter("newpassword");
		boolean flag = false;
		if(o != null && !StringUtils.isNullOrEmpty(newpassword)){
			UserService userService = new UserServiceImpl();
			flag = userService.updatePwd(((User)o).getId(),newpassword);
			if(flag){
				request.setAttribute(Constants.SYS_MESSAGE, "修改密码成功,请退出并使用新密码重新登录！");
				request.getSession().removeAttribute(Constants.USER_SESSION);//session注销
			}else{
				request.setAttribute(Constants.SYS_MESSAGE, "修改密码失败！");
			}
		}else{
			request.setAttribute(Constants.SYS_MESSAGE, "修改密码失败！");
		}
		request.getRequestDispatcher("pwdmodify.jsp").forward(request, response);
	}
	//修改密码时验证旧密码是否与原密码相等  他和更新密码时完全不同的两部分而且他没有调用service只是将serssino中的user掉出来
	private void getPwdByUserId(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Object o = request.getSession().getAttribute(Constants.USER_SESSION);
		String oldpassword = request.getParameter("oldpassword");
		Map<String, String> resultMap = new HashMap<String, String>();
		
		if(null == o ){//session过期
			resultMap.put("result", "sessionerror");
		}else if(StringUtils.isNullOrEmpty(oldpassword)){//旧密码输入为空
			resultMap.put("result", "error");
		}else{
			String sessionPwd = ((User)o).getUserPassword();
			if(oldpassword.equals(sessionPwd)){
				resultMap.put("result", "true");
			}else{//旧密码输入不正确
				resultMap.put("result", "false");
			}
		}
		
		response.setContentType("application/json");
		PrintWriter outPrintWriter = response.getWriter();
		outPrintWriter.write(JSONArray.toJSONString(resultMap));
		outPrintWriter.flush();
		outPrintWriter.close();
	}
	
	//更新操作获取用户的基本信息进行更新
	private void modify(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String id = request.getParameter("uid");
		String userName = request.getParameter("userName");
		String gender = request.getParameter("gender");
		String birthday = request.getParameter("birthday");
		String phone = request.getParameter("phone");
		String address = request.getParameter("address");
		String userRole = request.getParameter("userRole");
		
		User user = new User();
		user.setId(Integer.valueOf(id));
		user.setUserName(userName);
		user.setGender(Integer.valueOf(gender));
		try {
			user.setBirthday(new SimpleDateFormat("yyyy-MM-dd").parse(birthday));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		user.setPhone(phone);
		user.setAddress(address);
		user.setUserRole(Integer.valueOf(userRole));
		user.setModifyBy(((User)request.getSession().getAttribute(Constants.USER_SESSION)).getId());
		user.setModifyDate(new Date());
		
		UserService userService = new UserServiceImpl();
		if(userService.modify(user)){
			response.sendRedirect(request.getContextPath()+"/jsp/user.do?method=query");
		}else{
			request.getRequestDispatcher("usermodify.jsp").forward(request, response);
		}
	
	}
	//根据输入的用户id来查询某个用户而且Dao层用到了联表查询
	private void getUserById(HttpServletRequest request, HttpServletResponse response,String url)
			throws ServletException, IOException {
		String id = request.getParameter("uid");
		if(!StringUtils.isNullOrEmpty(id)){
			//调用后台方法得到user对象
			UserService userService = new UserServiceImpl();
			User user = userService.getUserById(id);
			request.setAttribute("user", user);
			request.getRequestDispatcher(url).forward(request, response);
		}
		
	}
	//删除用户
	private void delUser(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String id = request.getParameter("uid");
		Integer delId =0;
		try{
			delId = Integer.parseInt(id);
		}catch (Exception e) {
			// TODO: handle exception
			delId = 0;
		}
		//这里和验证旧密码一样要用到json
		HashMap<String, String> resultMap = new HashMap<String, String>();
		//增强程序的健壮性
		if(delId<0){
			resultMap.put("delResult","noexist");
		}else {
			//调用service层
			UserServiceImpl userService=new UserServiceImpl();
			boolean b = userService.deleteUserById(delId);
			if(b){
				resultMap.put("delResult", "true");
			}else {
				resultMap.put("delResult", "false");
			}
		}
		//我们在后端写完了如何让前端知道我们刚了什么呢 通过json
		response.setContentType("application/json");
		PrintWriter writer = response.getWriter();
		writer.write(JSONArray.toJSONString(resultMap));
		writer.flush();
		writer.close();
	}
	//通过用户的userCode来获取用户
	private void userCodeExist(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		//判断用户账号是否可用
		String userCode = request.getParameter("userCode");
		
		HashMap<String, String> resultMap = new HashMap<String, String>();
		if(StringUtils.isNullOrEmpty(userCode)){
			//userCode == null || userCode.equals("")
			resultMap.put("userCode", "exist");
		}else{
			UserService userService = new UserServiceImpl();
			User user = userService.selectUserCodeExist(userCode);
			if(null != user){
				resultMap.put("userCode","exist");
			}else{
				resultMap.put("userCode", "notexist");
			}
		}
		
		//把resultMap转为json字符串以json的形式输出
		//配置上下文的输出类型
		response.setContentType("application/json");
		//从response对象中获取往外输出的writer对象
		PrintWriter outPrintWriter = response.getWriter();
		//把resultMap转为json字符串 输出
		outPrintWriter.write(JSONArray.toJSONString(resultMap));
		outPrintWriter.flush();//刷新
		outPrintWriter.close();//关闭流
	}
	//获取用户列表
	private void getRoleList(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		List<Role> roleList = null;
		RoleService roleService = new RoleServiceImpl();
		roleList = roleService.getRoleList();
		//把roleList转换成json对象输出
		response.setContentType("application/json");
		PrintWriter outPrintWriter = response.getWriter();
		outPrintWriter.write(JSONArray.toJSONString(roleList));
		outPrintWriter.flush();
		outPrintWriter.close();
	}
	//用户分页
	private void query(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		//查询用户列表
		String queryUserName = request.getParameter("queryname");
		String temp = request.getParameter("queryUserRole");
		String pageIndex = request.getParameter("pageIndex");
		int queryUserRole = 0;
		UserService userService = new UserServiceImpl();
		List<User> userList = null;
		//设置页面容量
    	int pageSize = Constants.pageSize;
    	//当前页码
    	int currentPageNo = 1;
		System.out.println("queryUserName servlet--------"+queryUserName);  
		System.out.println("queryUserRole servlet--------"+queryUserRole);  
		System.out.println("query pageIndex--------- > " + pageIndex);
		if(queryUserName == null){
			queryUserName = "";
		}
		if(temp != null && !temp.equals("")){
			queryUserRole = Integer.parseInt(temp);
		}
		
    	if(pageIndex != null){
    		try{
    			currentPageNo = Integer.valueOf(pageIndex);
    		}catch(NumberFormatException e){
    			response.sendRedirect("error.jsp");
    		}
    	}	
    	//总数量（表）	
    	int totalCount	= userService.getUserCount(queryUserName,queryUserRole);
    	//总页数
    	PageSupport pages=new PageSupport();
    	pages.setCurrentPageNo(currentPageNo);
    	pages.setPageSize(pageSize);
    	pages.setTotalCount(totalCount);
    	//通过上面的set可以计算出总的页数是多少
    	int totalPageCount = pages.getTotalPageCount();
    	
    	//控制首页和尾页
    	if(currentPageNo < 1){
    		currentPageNo = 1;
    	}else if(currentPageNo > totalPageCount){
    		currentPageNo = totalPageCount;
    	}
		
		//获取用户的基本信息 但是此项是不在一张表上的需要去进行联表查询
		userList = userService.getUserList(queryUserName,queryUserRole,currentPageNo, pageSize);
		request.setAttribute("userList", userList);
		List<Role> roleList = null;
		//这个放入list里面是用来获取下拉选项的
		RoleService roleService = new RoleServiceImpl();
		roleList = roleService.getRoleList();
		request.setAttribute("roleList", roleList);

	/*	request.setAttribute("queryUserName", queryUserName);
		request.setAttribute("queryUserRole", queryUserRole);*/
		//这是下面分页的信息
		request.setAttribute("totalPageCount", totalPageCount);
		request.setAttribute("totalCount", totalCount);
		request.setAttribute("currentPageNo", currentPageNo);
		request.getRequestDispatcher("userlist.jsp").forward(request, response);
	}
	//添加用户
	private void add(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		//从前端来获取用户的信息
		String userCode = request.getParameter("userCode");
		String userName = request.getParameter("userName");
		String userPassword = request.getParameter("userPassword");
		String gender = request.getParameter("gender");
		String birthday = request.getParameter("birthday");
		String phone = request.getParameter("phone");
		String address = request.getParameter("address");
		String userRole = request.getParameter("userRole");
		//获取user对象，将user对象装进去
		User user=new User();
		user.setUserCode(userCode);
		user.setUserName(userName);
		user.setUserPassword(userPassword);
		user.setAddress(address);
		user.setGender(Integer.valueOf(gender));
		user.setPhone(phone);
		user.setUserRole(Integer.valueOf(userRole));
		user.setCreationDate(new Date());
		user.setCreatedBy(((User)request.getSession().getAttribute(Constants.USER_SESSION)).getId());
		try {
			user.setBirthday(new SimpleDateFormat("yyyy-MM-dd").parse(birthday));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		//调用userservice层来讲对象进过去
		UserServiceImpl userService=new UserServiceImpl();
		boolean add = userService.add(user);
		//这个时候就要判断是否添加成功了
		if(add){
			response.sendRedirect(request.getContextPath()+"/jsp/user.do?method=query");
		}else {
			request.getRequestDispatcher("useradd.jsp").forward(request, response);
		}
	}
	/**
	 * Initialization of the servlet. <br>
	 *
	 * @throws ServletException if an error occurs
	 */
	public void init() throws ServletException {
		// Put your code here
	}

}
