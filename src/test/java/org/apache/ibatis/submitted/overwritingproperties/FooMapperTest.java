package org.apache.ibatis.submitted.overwritingproperties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.*;
import org.junit.*;

import java.io.Reader;
import java.sql.Connection;

/**
 * @author jjensen
 */
public class FooMapperTest {

  private final static String SQL_MAP_CONFIG = "org/apache/ibatis/submitted/overwritingproperties/sqlmap.xml";
  private static SqlSession session;

  @BeforeClass
  public static void setUpBeforeClass() {
    try {
      final SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader(SQL_MAP_CONFIG));
      session = factory.openSession();
      Connection conn = session.getConnection();
      ScriptRunner runner = new ScriptRunner(conn);
      Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/overwritingproperties/create-schema-mysql.sql");
      runner.runScript(reader);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Before
  public void setUp() {
    final FooMapper mapper = session.getMapper(FooMapper.class);
    mapper.deleteAllFoo();
    session.commit();
  }

  @Test
  public void testOverwriteWithDefault() {
    final FooMapper mapper = session.getMapper(FooMapper.class);
    final Bar bar = new Bar(2L);
    final Foo inserted = new Foo(1L, bar, 3, 4);
    mapper.insertFoo(inserted);

    final Foo selected = mapper.selectFoo();
    
    // field1 is explicitly mapped properly
    // <result property="field1" column="field1" jdbcType="INTEGER"/>
    Assert.assertEquals(inserted.getField1(), selected.getField1());

    // field4 is not mapped in the result map
    // <result property="field4" column="field3" jdbcType="INTEGER"/>
    Assert.assertEquals(inserted.getField3(), selected.getField4() );

    // field4 is explicitly remapped to field3 in the resultmap
    // <result property="field3" column="field4" jdbcType="INTEGER"/>
    Assert.assertEquals(inserted.getField4(), selected.getField3());

    // is automapped from the only column that matches... which is Field1
    // probably not the intention, but it's working correctly given the code
    // <association property="field2" javaType="Bar">
    //  <result property="field1" column="bar_field1" jdbcType="INTEGER"/>
    // </association>
    Assert.assertEquals(inserted.getField2().getField1(), selected.getField2().getField1());
  }

  @AfterClass
  public static void tearDownAfterClass() {
    session.close();
  }

}