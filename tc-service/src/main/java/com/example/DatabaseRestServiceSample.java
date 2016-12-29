package com.example;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.example.auth.Auth;
import com.example.filters.CORS;
import com.example.model.Employee;

/**
 * REST -> JAX-RS -> JPA (-> JDBC)簡単解説版
 * */
@Path("/hr-sample")
public class DatabaseRestServiceSample {

    private EntityManagerFactory emf = DatabaseService.getInstance().getEntityManagerFactory();

    // 全従業員情報を取得するRESTサービス - POJOにアノテーション
    @GET
    @Path("/employees")
    @Produces(MediaType.APPLICATION_JSON)
    // @Auth // 認証処理を挿入するContainerRequestFilter
    // @CORS // CORSヘッダを付加するContainerResponseFilter
    public Employee[] getEmployees() throws Exception{

      EntityManager em = emf.createEntityManager();
      try{

        // Criteria Queryを使った従業員情報の取得 - JPQLもNative Queryも書かず
        CriteriaBuilder cb = em.getCriteriaBuilder();
        List<Employee> empList = em.createQuery(cb.createQuery(Employee.class)).getResultList();
        return empList.toArray(new Employee[empList.size()]);

      }finally{
        if(null != em) em.close();
      }
    }
}


/*
        // 方法１： Criteria Queryを使った従業員情報の取得
        List<Employee> empList = em.createQuery(em.getCriteriaBuilder().createQuery(Employee.class)).getResultList();

        // 方法２： JPQLを使った従業員情報の取得
        List<Employee> empList = em.createQuery("select e from Employee e order by e.employeeId").getResultList();

        // 方法３： Native Queryを使った従業員情報の取得
        List<Employee> empList = em.createNativeQuery("SELECT * FROM EMPLOYEES", Employee.class).getResultList();

*/
