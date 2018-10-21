package util;

public enum Words {
    OWL_CLASS,             //定义类词汇
    OWL_DATATYPEPROPERTY,  //定义数据类型属性词汇
    OWL_OBJECTPROPERTY,    //定义对象属性词汇
    OWL_NAMEDINDIVIDUAL,   //定义实例词汇
    OWL_TOPDATAPROPERTY,
    OWL_TOPOBJECTPROPERTY,
    RDF_TYPE,       //定义词汇
    RDFS_DOMAIN,    //声明定义域
    RDFS_RANGE,     //声明值域
    RDFS_LABEL,     //声明可读名
    RDFS_COMMENT,   //声明含义
    RDFS_SUBCLASSOF,  //父子类关系词汇
    OWL_EQCLASS,    //定义等价类词汇
    OWL_DJCLASS,     //定义不相交类词汇
    RDFS_SUBPROPERTYOF,  //父子属性关系词汇
    OWL_EQPROPERTY,     //等价属性
    OWL_DJPROPERTY,    //不相交属性
    OWL_IVPROPERTY,    //互逆属性
    OWL_SAME_AS,      //相等实例
    OWL_DFINS      //不等实例
}
