package com.zlebank.zplatform.acc.bean;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.property.ChainedPropertyAccessor;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.transform.ResultTransformer;

/**
 * 
 * @author luxiaoshuai
 *
 */
public class SQLColumnToBean implements ResultTransformer {
	private static final Log logger = LogFactory.getLog(SQLColumnToBean.class);
	private static final long serialVersionUID = 1L;
	private final Class<?> resultClass;
	private Setter[] setters;
	private PropertyAccessor propertyAccessor;

	public SQLColumnToBean(Class<?> resultClass) {
		if (resultClass == null)
			throw new IllegalArgumentException("resultClass cannot be null");
		this.resultClass = resultClass;
		propertyAccessor = new ChainedPropertyAccessor(new PropertyAccessor[] {
				PropertyAccessorFactory.getPropertyAccessor(resultClass, null),
				PropertyAccessorFactory.getPropertyAccessor("field") });
	}

	// 结果转换时，HIBERNATE调用此方法
	public Object transformTuple(Object[] tuple, String[] aliases) {
		Object result = null;

		try {
			if (setters == null) {// 首先初始化，取得目标POJO类的所有SETTER方法
				setters = new Setter[aliases.length];
				// 注意，如果在创建SQL的时候用了query.addScalar(columnAlias)
				// 的话，这里仅仅会出现addScalar指定的列。
				for (int i = 0; i < aliases.length; i++) {
					String alias = aliases[i];
					if (alias != null && !"ROWNUM_".equals(alias)) {
						// 我的逻辑主要是在getSetterByColumnName方法里面，其它都是HIBERNATE的另一个类中COPY的
						// 这里填充所需要的SETTER方法
						setters[i] = getSetterByColumnName(alias);
					}
				}
			}
			result = resultClass.newInstance();

			// 这里使用SETTER方法填充POJO对象
			for (int i = 0; i < aliases.length; i++) {
				if (setters[i] != null) {
					setters[i].set(result, tuple[i], null);
				}
			}
		} catch (InstantiationException e) {
			if(logger.isInfoEnabled()){
				logger.error("Could not instantiate resultclass: "+ resultClass.getName());
				logger.error(e.getMessage());
			}
			throw new HibernateException("Could not instantiate resultclass: "
					+ resultClass.getName());
		} catch (IllegalAccessException e) {
			if(logger.isInfoEnabled()){
				logger.error("Could not instantiate resultclass: "+ resultClass.getName());
				logger.error(e.getMessage());
			}
			throw new HibernateException("Could not instantiate resultclass: "
					+ resultClass.getName());
		} catch (HibernateException e) {
			if(logger.isInfoEnabled()){
				logger.error(e.getMessage());
			}
			e.printStackTrace();
		}

		return result;
	}

	// 根据数据库字段名在POJO查找JAVA属性名，参数就是数据库字段名，如：USER_ID
	private Setter getSetterByColumnName(String alias) {
		// 取得POJO所有属性名
		Field[] fields = resultClass.getDeclaredFields();
		if (fields == null || fields.length == 0) {
			throw new RuntimeException("实体" + resultClass.getName() + "不含任何属性");
		}
		// 把字段名中所有的下杠去除
		String proName = alias.replaceAll("_", "").toLowerCase();
		for (Field field : fields) {
			if (field.getName().toLowerCase().equals(proName)) {// 去除下杠的字段名如果和属性名对得上，就取这个SETTER方法
				return propertyAccessor.getSetter(resultClass, field.getName());
			}
		}
		throw new RuntimeException(
				"找不到数据库字段 ："
						+ alias
						+ " 对应的POJO属性或其getter方法，比如数据库字段为USER_ID或USERID，那么JAVA属性应为userId");
	}

	@SuppressWarnings("rawtypes")
	public List transformList(List collection) {
		return collection;
	}
}
