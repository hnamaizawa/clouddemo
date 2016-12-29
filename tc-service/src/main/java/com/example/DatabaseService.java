package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.model.Department;
import com.example.model.Employee;

import javax.persistence.criteria.Predicate;


/**
 * 
 */
public class DatabaseService {

	private static Logger logger = LoggerFactory.getLogger(DatabaseService.class);

	private static final String PERSISTENCE_UNIT = "simple-jpa-service";
	private static final String DBAAS_DEFAULT_CONNECT_DESCRIPTOR = System.getenv("DBAAS_DEFAULT_CONNECT_DESCRIPTOR");
	private static final String DBAAS_USER_NAME = System.getenv("DBAAS_USER_NAME");
	private static final String DBAAS_USER_PASSWORD = System.getenv("DBAAS_USER_PASSWORD");
	
	private static DatabaseService service;

	private EntityManagerFactory emf;
	//private EntityManager em;
	
	public static synchronized DatabaseService getInstance(){
		if(null == service){
			service = new DatabaseService();
	        Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override
				public void run() {
					if(null != service){
						service.close();
						logger.info("JPA service shutdown.");
					}
				}
	        });
		}
		return service;
	}

	public DatabaseService(){
		Map<String, String> map = new HashMap<String, String>();

		if(null != DBAAS_DEFAULT_CONNECT_DESCRIPTOR){
			map.put("javax.persistence.jdbc.url", "jdbc:oracle:thin:@//" + DBAAS_DEFAULT_CONNECT_DESCRIPTOR);
			logger.info("jdbc.url: " + DBAAS_DEFAULT_CONNECT_DESCRIPTOR);
		}
		if(null != DBAAS_USER_NAME){
			map.put("javax.persistence.jdbc.user", DBAAS_USER_NAME);
		}
		if(null != DBAAS_USER_PASSWORD){
			map.put("javax.persistence.jdbc.password", DBAAS_USER_PASSWORD);
		}
		//map.put("javax.persistence.jdbc.driver", "oracle.jdbc.OracleDriver");
	
		emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, map);
		
		//Map<String, Object> props = emf.getProperties();
		
	}
	
	public synchronized void close(){
		if(null != emf){
			emf.close();
			emf = null;
		}
	}
	
	public EntityManagerFactory getEntityManagerFactory(){
		return emf;
	}

	
	public List<Employee> getEmployees() throws Exception{

		EntityManager em = emf.createEntityManager();
		try{
			if(0 == System.currentTimeMillis()/50 % 10){
				return em.createNativeQuery("SELECT * FROM EEMPLOYEESS", Employee.class).getResultList();
				//throw new Exception("Intentional Error for DEMO.");
			}

			
			long l = System.currentTimeMillis()/500 % 7 + 2;
			return em.createQuery("select e from Employee e where MOD(e.employeeId, " + l + ") != 0 order by e.employeeId").getResultList();
			//return em.createQuery("select e from Employee e order by e.employeeId").getResultList();
			//return em.createNativeQuery("SELECT * FROM EMPLOYEES", Employee.class).getResultList();
		}finally{
			if(null != em) em.close();
		}
	}

	
	public Employee getEmployee(int empno){
		EntityManager em = emf.createEntityManager();
		try{
			//Query q = em.createNativeQuery("SELECT * FROM HR.EMPLOYEES WHERE EMPLOYEE_ID = ?1", Employee.class);
			Query q = em.createQuery("select e from Employee e where e.employeeId = ?1");
			q.setParameter(1, empno);
			Employee emp = (Employee)q.getSingleResult();
			return emp;

		}finally{
			if(null != em) em.close();
		}
	}
	
/*************	
	public Emp getManager(Emp emp){
		return getManager(emp.getEmpno().intValue());
	}
	
	public Emp getManager(int empno){
		EntityManager em = emf.createEntityManager();
		try{
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Emp> c = cb.createQuery(Emp.class);
			Root<Emp> emp = c.from(Emp.class);
			Root<Emp> emp_man = c.from(Emp.class);

			Predicate criteria = cb.equal(emp.get(Emp_.mgr), emp_man.get(Emp_.empno));
			criteria = cb.and(criteria, cb.equal(emp.get(Emp_.empno), empno));
			c.where(criteria);
			c.select(emp_man);
			
			TypedQuery<Emp> q = em.createQuery(c);
			Emp result = q.getSingleResult();
			return result;

		}finally{
			if(null != em) em.close();
		}
	}


	public List<Emp> findEmpByName(String equals, String contains){
		EntityManager em = emf.createEntityManager();
		try{
			//cb.or(cb.like(e.get("firstName"), "A%")
			
			//Query q = em.createNativeQuery("SELECT * FROM EMP", Emp.class);
			//List<Emp> empList = (List<Emp>)q.getResultList();
			
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Emp> cq = cb.createQuery(Emp.class);
			Root<Emp> root = cq.from(Emp.class);

			if(null != equals && equals.length() > 0){
				cq.where(cb.equal(cb.upper(root.get(Emp_.ename)), equals.toUpperCase()));
			}else if(null != contains && contains.length() > 0){
				cq.where(cb.like(cb.upper(root.get(Emp_.ename)), "%" + contains.toUpperCase() + "%"));
			}else{
				return new ArrayList<Emp>(); // empty
			}
			
			TypedQuery<Emp> query = em.createQuery(cq);
			List<Emp> empList = query.getResultList();
			return empList;

		}finally{
			if(null != em) em.close();
		}
	}
**************/	
	
	public List<Department> getDepartments(){
		EntityManager em = emf.createEntityManager();
		try{
			Query q = em.createQuery("select d from Department d");
			return (List<Department>)q.getResultList();
		}finally{
			if(null != em) em.close();
		}

	}

	
}


/*
public DaoServiceBase(String persistenceUnit){
	log = LogFactory.getLog(this.getClass());
	emf = Persistence.createEntityManagerFactory(persistenceUnit);
	em = emf.createEntityManager();
	em.setFlushMode(FlushModeType.COMMIT);
	// If there is no transaction active, the persistence provider must not flush to the database. 
}

public DaoServiceBase(String persistenceUnit, @SuppressWarnings("rawtypes") Map props){
	emf = Persistence.createEntityManagerFactory(persistenceUnit, props);
	em = emf.createEntityManager();
	em.setFlushMode(FlushModeType.COMMIT);
	// If there is no transaction active, the persistence provider must not flush to the database. 
} 
*/