package com.jackylaucf.jcriteria;

import com.jackylaucf.jcriteria.annotation.Criteria;
import com.jackylaucf.jcriteria.annotation.TargetEntity;
import com.jackylaucf.jcriteria.criteria.QueryCriteria;

import java.lang.reflect.Field;
import java.util.*;

class PersistenceIO {

    private QueryCriteria criteria;
    private List<String> conditionNameList;

    class JPQLWriter{
        static final String SPACE = " ";
        static final String ALIAS = "r";
        static final String DOT = ".";
        static final String SELECT = " SELECT ";
        static final String FROM = " FROM ";
        static final String WHERE = " WHERE ";
        static final String COUNT = "COUNT(" + ALIAS + ")";
        static final String WILDCARD = "%";
        static final String OPEN_BRACKET="(";
        static final String CLOSE_BRACKET=")";
        static final String NAMED_PARAMETER=":";

        private StringBuilder stringBuilder;
        private Map<String, Object> criteriaValueMap;

        private JPQLWriter(){
            stringBuilder = new StringBuilder();
            criteriaValueMap = new HashMap<>();
        }

        String getJPQL(){
            return stringBuilder.toString();
        }

        Map<String, Object> getWriterValueMap(){
            return this.criteriaValueMap;
        }

        private void writeFromStatement(){
            stringBuilder.append(JPQLWriter.FROM);
            TargetEntity targetEntity = criteria.getClass().getAnnotation(TargetEntity.class);
            if (targetEntity == null) {
                throw new IllegalArgumentException();
            }
            else {
                stringBuilder.append(targetEntity.value().getSimpleName()).append(JPQLWriter.SPACE).append(JPQLWriter.ALIAS);
            }
        }

        private void writeWhereStatement() throws NoSuchFieldException, IllegalAccessException {
            List<Field> fields = new ArrayList<>();
            if(conditionNameList==null){
                fields = Arrays.asList(criteria.getClass().getDeclaredFields());
            }else{
                for (String fieldName : conditionNameList) {
                    fields.add(criteria.getClass().getDeclaredField(fieldName));
                }
            }
            for(int i=0,j=0; i<fields.size(); i++){
                Field f = fields.get(i);
                f.setAccessible(true);
                Object o = f.get(criteria);
                if(o!=null){
                    criteriaValueMap.put(f.getName(), o);
                    prepareStatement(f, j);
                    j++;
                }
            }
        }

        private void prepareStatement(Field field, int index){
            Criteria criteria = field.getDeclaredAnnotation(Criteria.class);
            if(index!=0){
                stringBuilder.append(criteria.externalConjunction().jpql());
            }
            if(criteria.mapTo().length>1){
                stringBuilder.append(OPEN_BRACKET);
                for(int i=0; i<criteria.mapTo().length; i++){
                    stringBuilder.append(ALIAS).append(DOT).append(criteria.mapTo()[i]).append(criteria.logic().jpql());
                    stringBuilder.append(NAMED_PARAMETER).append(field.getName());
                    if(i!=criteria.mapTo().length-1){
                        stringBuilder.append(criteria.internalConjunction().jpql());
                    }
                }
                stringBuilder.append(CLOSE_BRACKET);
            }else if(criteria.mapTo().length==1){
                stringBuilder.append(ALIAS).append(DOT).append(criteria.mapTo()[0]).append(criteria.logic().jpql());
                stringBuilder.append(NAMED_PARAMETER).append(field.getName());
            }
        }
    }

    PersistenceIO(QueryCriteria criteria){
        this.criteria = criteria;
    }

    PersistenceIO(QueryCriteria criteria, List<String> conditionNameList){
        this.criteria = criteria;
        this.conditionNameList = conditionNameList;
    }

    JPQLWriter getWriter() throws NoSuchFieldException, IllegalAccessException {
        JPQLWriter writer = new JPQLWriter();
        writer.writeFromStatement();
        writer.writeWhereStatement();
        return writer;
    }
}
