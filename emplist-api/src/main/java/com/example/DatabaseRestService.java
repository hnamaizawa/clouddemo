package com.example;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.auth.Auth;
import com.example.filters.CORS;
import com.example.model.Department;
import com.example.model.Employee;

@Path("/hr")
public class DatabaseRestService {

		//private Logger logger = LoggerFactory.getLogger(getClass());
		private DatabaseService dbservice = DatabaseService.getInstance();

		// 全従業員情報を取得する //
		@GET
		@Path("/employees")
		@Produces(MediaType.APPLICATION_JSON)
		@Auth // 認証処理を挿入するContainerRequestFilter
		@CORS // CORSヘッダを付加するContainerResponseFilter
    public Employee[] getEmployees() throws Exception{

			List<Employee> list = dbservice.getEmployees();
			return list.toArray(new Employee[list.size()]);
		}

}
