package com.logan.demo.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;


@Component
public class SpringBeanUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringBeanUtils.applicationContext == null) {
            SpringBeanUtils.applicationContext = applicationContext;

            System.out.println("----------------------------------------------------------------------------------------");
            System.out.println("ApplicationContext=" + SpringBeanUtils.applicationContext);
            System.out.println("----------------------------------------------------------------------------------------");
        }
    }


    /**
     * 获取applicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }


    /**
     * 通过name获取 Bean
     */
    public static Object getBean(String name) {
        try {
            return getApplicationContext().getBean(name);
        } catch (Exception e) {
        }
        return null;
    }


    /**
     * 通过class获取Bean
     */
    public static <T> T getBean(Class<T> clazz) {
        try {
            return getApplicationContext().getBean(clazz);
        } catch (Exception e) {
        }
        return null;
    }


    /**
     * 通过name以及Clazz返回指定的Bean
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        try {
            return getApplicationContext().getBean(name, clazz);
        } catch (Exception e) {
        }
        return null;
    }

}

