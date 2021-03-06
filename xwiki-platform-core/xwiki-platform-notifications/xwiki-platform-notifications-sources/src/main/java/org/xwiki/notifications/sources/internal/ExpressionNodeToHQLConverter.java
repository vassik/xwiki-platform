/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.notifications.sources.internal;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.filters.expression.AndNode;
import org.xwiki.notifications.filters.expression.BooleanValueNode;
import org.xwiki.notifications.filters.expression.DateValueNode;
import org.xwiki.notifications.filters.expression.EntityReferenceNode;
import org.xwiki.notifications.filters.expression.EqualsNode;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.expression.GreaterThanNode;
import org.xwiki.notifications.filters.expression.InNode;
import org.xwiki.notifications.filters.expression.LesserThanNode;
import org.xwiki.notifications.filters.expression.StartsWith;
import org.xwiki.notifications.filters.expression.NotEqualsNode;
import org.xwiki.notifications.filters.expression.NotNode;
import org.xwiki.notifications.filters.expression.OrNode;
import org.xwiki.notifications.filters.expression.PropertyValueNode;
import org.xwiki.notifications.filters.expression.StringValueNode;
import org.xwiki.notifications.filters.expression.generics.AbstractBinaryOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractUnaryOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractValueNode;
import org.xwiki.text.StringUtils;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

/**
 * Converter used to transform {@link ExpressionNode} based abstract syntax trees to HQL language.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component(roles = ExpressionNodeToHQLConverter.class)
@Singleton
public class ExpressionNodeToHQLConverter
{
    /**
     * The HQL query with its parameters generated by the converter.
     */
    public static final class HQLQuery
    {
        private String query;

        private Map<String, Object> queryParameters = new HashMap<>();

        /**
         * @return the HQL query
         */
        public String getQuery()
        {
            return query;
        }

        /**
         * @return the parameters to bind to the HQL query
         */
        public Map<String, Object> getQueryParameters()
        {
            return queryParameters;
        }
    }

    private static final String VARIABLE_NAME = ":%s";

    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Convert an ExpressionNode to an HQLQuery.
     * @param node the node to convert
     * @return the generated HQL query
     */
    public HQLQuery parse(ExpressionNode node)
    {
        HQLQuery result = new HQLQuery();
        result.query = parseBlock(node, result);
        return result;
    }

    private String parseBlock(ExpressionNode node, HQLQuery result)
    {
        if (node instanceof AbstractValueNode) {
            return parseValue((AbstractValueNode) node, result);
        } else if (node instanceof AbstractUnaryOperatorNode) {
            return parseUnaryOperator((AbstractUnaryOperatorNode) node, result);
        } else if (node instanceof AbstractBinaryOperatorNode) {
            return parseBinaryOperator((AbstractBinaryOperatorNode) node, result);
        } else if (node instanceof AbstractOperatorNode) {
            return parseOtherOperation((AbstractOperatorNode) node, result);
        } else {
            return StringUtils.EMPTY;
        }
    }

    private String parseValue(AbstractValueNode value, HQLQuery result)
    {
        return parseValue(value, false, result);
    }

    private String parseValue(AbstractValueNode value, boolean escape, HQLQuery result)
    {
        String returnValue;

        if (value instanceof PropertyValueNode) {
            switch (((PropertyValueNode) value).getContent()) {
                case ID:
                    returnValue = "event.id";
                    break;
                case GROUP_ID:
                    returnValue = "event.requestId";
                    break;
                case STREAM:
                    returnValue = "event.stream";
                    break;
                case DATE:
                    returnValue = "event.date";
                    break;
                case APPLICATION:
                    returnValue = "event.application";
                    break;
                case BODY:
                    returnValue = "event.body";
                    break;
                case TYPE:
                    returnValue = "event.type";
                    break;
                case HIDDEN:
                    returnValue = "event.hidden";
                    break;
                case PAGE:
                    returnValue = "event.page";
                    break;
                case IMPORTANCE:
                    returnValue = "event.priority";
                    break;
                case SPACE:
                    returnValue = "event.space";
                    break;
                case TITLE:
                    returnValue = "event.title";
                    break;
                case USER:
                    returnValue = "event.user";
                    break;
                case WIKI:
                    returnValue = "event.wiki";
                    break;
                case URL:
                    returnValue = "event.url";
                    break;
                case DOCUMENT_VERSION:
                    returnValue = "event.version";
                    break;
                default:
                    returnValue = StringUtils.EMPTY;
            }
        } else if (value instanceof StringValueNode) {
            // If we’re dealing with raw values, we have to put them in the queryParameters map
            StringValueNode valueNode = (StringValueNode) value;
            String nodeContent = (escape) ? escape(valueNode.getContent()) : valueNode.getContent();

            // In order to lower the probability of having collisions in the query parameters provided by other
            // parsers, we use a key based on the sha256 fingerprint of its value.
            String mapKey = String.format("value_%s", sha256Hex(valueNode.getContent()));

            result.queryParameters.put(mapKey, nodeContent);

            returnValue = String.format(VARIABLE_NAME, mapKey);
        } else if (value instanceof EntityReferenceNode) {
            String stringValue = serializer.serialize(((EntityReferenceNode) value).getContent());
            if (escape) {
                stringValue = escape(stringValue);
            }

            String mapKey = String.format("entity_%s",  sha256Hex(stringValue));

            result.queryParameters.put(mapKey, stringValue);

            returnValue = String.format(VARIABLE_NAME, mapKey);
        } else if (value instanceof DateValueNode) {
            DateValueNode dateValueNode = (DateValueNode) value;
            String stringValue = dateValueNode.getContent().toString();

            String mapKey = String.format("date_%s",  sha256Hex(stringValue));

            result.queryParameters.put(mapKey, dateValueNode.getContent());

            returnValue = String.format(VARIABLE_NAME, mapKey);
        } else if (value instanceof BooleanValueNode) {
            returnValue = ((BooleanValueNode) value).getContent().toString();
        } else {
            returnValue = StringUtils.EMPTY;
        }

        return returnValue;
    }

    private String parseUnaryOperator(AbstractUnaryOperatorNode operator, HQLQuery result)
    {
        if (operator instanceof NotNode) {
            return String.format(" NOT (%s)", parseBlock(operator.getOperand(), result));
        } else {
            return StringUtils.EMPTY;
        }
    }

    private String parseBinaryOperator(AbstractBinaryOperatorNode operator, HQLQuery result)
    {
        String returnValue;

        if (operator instanceof AndNode) {
            returnValue = String.format("(%s) AND (%s)", parseBlock(operator.getLeftOperand(), result),
                    parseBlock(operator.getRightOperand(), result));
        } else if (operator instanceof OrNode) {
            returnValue = String.format("(%s) OR (%s)", parseBlock(operator.getLeftOperand(), result),
                    parseBlock(operator.getRightOperand(), result), result);
        } else if (operator instanceof EqualsNode) {
            returnValue = String.format("%s = %s", parseValue((AbstractValueNode) operator.getLeftOperand(), result),
                    parseValue((AbstractValueNode) operator.getRightOperand(), result));
        } else if (operator instanceof NotEqualsNode) {
            returnValue = String.format("%s <> %s", parseValue((AbstractValueNode) operator.getLeftOperand(), result),
                    parseValue((AbstractValueNode) operator.getRightOperand(), result));
        } else if (operator instanceof StartsWith) {
            returnValue = String.format("%s LIKE concat(%s, '%%') ESCAPE '!'",
                    parseValue((AbstractValueNode) operator.getLeftOperand(), result),
                    parseValue((AbstractValueNode) operator.getRightOperand(), true, result));
        } else if (operator instanceof GreaterThanNode) {
            returnValue = String.format("%s >= %s", parseBlock(operator.getLeftOperand(), result),
                    parseBlock(operator.getRightOperand(), result));
        } else if (operator instanceof LesserThanNode) {
            returnValue = String.format("%s <= %s", parseBlock(operator.getLeftOperand(), result),
                    parseBlock(operator.getRightOperand(), result));
        } else {
            returnValue = StringUtils.EMPTY;
        }

        return returnValue;
    }

    private String parseOtherOperation(AbstractOperatorNode operator, HQLQuery result)
    {
        String returnValue;

        if (operator instanceof InNode) {
            InNode inOperator = (InNode) operator;
            StringBuilder builder = new StringBuilder(parseBlock(inOperator.getLeftOperand(), result));
            builder.append(" IN (");

            String separator = "";
            for (AbstractValueNode value : inOperator.getValues()) {
                builder.append(separator);
                builder.append(parseBlock(value, result));
                separator = ", ";
            }

            builder.append(")");

            returnValue = builder.toString();
        } else if (operator instanceof OrderByNode) {
            OrderByNode orderByNode = (OrderByNode) operator;
            returnValue = String.format("%s ORDER BY %s %s", parseBlock(orderByNode.getQuery(), result),
                    parseBlock(orderByNode.getProperty(), result), orderByNode.getOrder().name());
        } else if (operator instanceof InListOfReadEventsNode) {
            InListOfReadEventsNode inList = (InListOfReadEventsNode) operator;

            returnValue = String.format("event IN (select status.activityEvent from ActivityEventStatusImpl status "
                    + "where status.activityEvent = event and status.entityId = :userStatusRead and status.read = true)"
            );

            result.getQueryParameters().put("userStatusRead", serializer.serialize(inList.getUser()));

        } else {
            returnValue = StringUtils.EMPTY;
        }

        return returnValue;
    }

    private String escape(String format)
    {
        // See EscapeLikeParametersQuery#convertParameters()
        return format.replaceAll("([%_!])", "!$1");
    }
}
