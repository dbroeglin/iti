package fr.broeglin.integration.iti.routes;

import java.math.BigInteger;
import java.util.Arrays;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.springframework.util.StringUtils;

import sbmappservices72.Auth;
import sbmappservices72.FieldIdentifier;
import sbmappservices72.MultipleResponseItemOptions;
import sbmappservices72.ObjectFactory;
import sbmappservices72.TableIdentifier;

public class ItiRouteBuilder extends RouteBuilder {

  private static final String OPTIONS = "OPTIONS";

  @Override
  public void configure() throws Exception {
    onException(Exception.class)
        .handled(true)
        .to("log:error?showAll=true&level=ERROR")
        .setBody(constant("{ \"code\": 000 }"));

    from("cxfrs:bean:rsServer?bindingStyle=SimpleConsumer")
        .to("log:request?showHeaders=true")
        .setHeader(CxfConstants.OPERATION_NAME, constant("GetItemsByQuery"))
        .setProperty(AUTH,
            createAuth(simple("{{iti.backend.username}}"), simple("{{iti.backend.password}}")))
        .setProperty(WHERE_CLAUSE,
            simpleFromProperty(simple("iti.cis.${header.ciType}.where")))
        .setProperty(ORDER_BY_CLAUSE,
            simpleFromProperty(simple("iti.cis.${header.ciType}.orderBy")))
        .setProperty(OPTIONS,
            createMultipleOptions(simpleFromProperty(simple("iti.cis.${header.ciType}.where"))))
        .setBody(createRequest())
        .to("{{iti.cis.endPoint}}")
        .to("log:responseProcessing?showHeaders=true")
        // .process(new Processor() {
        // @Override
        // public void process(Exchange ex) throws Exception {
        // ObjectMapper mapper = new ObjectMapper();
        // ObjectNode node = mapper.createObjectNode();
        //
        // node.put("_type", "coucou");
        //
        // ex.getIn().setBody(node);
        // }
        // })
        .removeHeaders("*")
        .to("log:output")
        .marshal("json");
  }

  // Plumbing

  private static final String AUTH = "AUTH";
  private static final String ORDER_BY_CLAUSE = "ORDER_BY_CLAUSE";
  private static final String WHERE_CLAUSE = "WHERE_CLAUSE";
  ObjectFactory factory = new ObjectFactory();

  private TableIdentifier createTableIdentifier(String dbName) {
    TableIdentifier id = factory.createTableIdentifier();

    id.setDbName(factory.createTableIdentifierDbName(dbName));

    return id;
  }

  private Expression createMultipleOptions(Expression exp) {

    return new Expression() {

      @SuppressWarnings("unchecked")
      @Override
      public <T> T evaluate(Exchange exchange, Class<T> type) {
        MultipleResponseItemOptions options = factory.createMultipleResponseItemOptions();
        String value = exp.evaluate(exchange, String.class);

        
        if (value != null) {
          for (String fieldName : value.split(",")) {
            options.getLimitedField().add(createFieldIdentifier(fieldName));
          }
        }
        return (T) options;
      }
    };
  }

  private FieldIdentifier createFieldIdentifier(String name) {
    FieldIdentifier fieldId = factory.createFieldIdentifier();

    fieldId.setDbName(factory.createFieldIdentifierDbName(name));

    return fieldId;
  }

  private Expression createAuth(final Expression usernameExp, final Expression passwordExp) {

    return new Expression() {

      @SuppressWarnings("unchecked")
      @Override
      public <T> T evaluate(Exchange exchange, Class<T> type) {
        Auth auth = new Auth();
        String username = usernameExp.evaluate(exchange, String.class);
        String password = passwordExp.evaluate(exchange, String.class);

        auth.setUserId(factory.createAuthUserId(username));
        auth.setPassword(factory.createAuthPassword(password));
        return (T) auth;
      }
    };
  }

  public Expression createRequest() {
    return new Expression() {
      @SuppressWarnings("unchecked")
      @Override
      public <T> T evaluate(Exchange exchange, Class<T> type) {
        Message msg = exchange.getIn();

        return (T) Arrays.asList(
            exchange.getProperty(AUTH),
            createTableIdentifier("DBNAME"),
            exchange.getProperty(WHERE_CLAUSE),
            exchange.getProperty(ORDER_BY_CLAUSE),
            BigInteger.valueOf(msg.getHeader("offset", Long.class)),
            BigInteger.valueOf(msg.getHeader("limit", Long.class)),
            exchange.getProperty(OPTIONS));
      }
    };
  }

  private Expression simpleFromProperty(Expression propertyExp) {
    return new Expression() {

      @SuppressWarnings("unchecked")
      @Override
      public <T> T evaluate(Exchange exchange, Class<T> type) {
        String template = "{{" + propertyExp.evaluate(exchange, String.class) + "}}";

        return (T) SimpleBuilder.simple(template).evaluate(exchange, String.class);
      }
    };
  }
}
