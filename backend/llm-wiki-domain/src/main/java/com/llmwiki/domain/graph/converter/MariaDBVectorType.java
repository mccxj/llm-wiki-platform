package com.llmwiki.domain.graph.converter;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.util.Arrays;

/**
 * Custom Hibernate UserType for MariaDB 11.8+ native VECTOR columns.
 * <p>
 * MariaDB's VECTOR type is not natively understood by standard JPA {@code AttributeConverter}
 * because the JDBC driver requires {@code PreparedStatement.setObject(index, float[])} — not
 * {@code setString()} — to write to a VECTOR column.
 * <p>
 * This UserType leverages MariaDB Connector/J 3.5+'s native VECTOR support:
 * <ul>
 *   <li><b>Write:</b> {@code PreparedStatement.setObject(index, float[])} — the JDBC driver
 *       serialises the float array into MariaDB's native VECTOR format internally.</li>
 *   <li><b>Read:</b> {@code ResultSet.getObject(column, float[].class)} — the JDBC driver
 *       deserialises the VECTOR value back into a Java float array.</li>
 * </ul>
 * <p>
 * The JPA entity field must also carry {@code @Column(columnDefinition = "VECTOR(1536)")}
 * so that Hibernate generates correct DDL for MariaDB. In H2-based tests the DDL will log a
 * warning and be skipped (H2 does not understand VECTOR, but the tests use Mockito mocks,
 * not real database access for vector operations).
 * <p>
 * <b>Replaces the deprecated {@link FloatArrayToJsonConverter}</b> which serialised vectors
 * as JSON strings — an approach that cannot interoperate with MariaDB's VECTOR type because
 * standard JDBC {@code setString()} does not invoke the required {@code VEC_FromText()} SQL
 * function on the server side.
 *
 * @see <a href="https://mariadb.com/kb/en/vector-types/">MariaDB VECTOR types</a>
 * @see <a href="https://mariadb.com/docs/server/connect/connectors/connector-j/">MariaDB Connector/J</a>
 */
public class MariaDBVectorType implements UserType<float[]> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public boolean equals(float[] x, float[] y) {
        return Arrays.equals(x, y);
    }

    @Override
    public int hashCode(float[] x) {
        return Arrays.hashCode(x);
    }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position,
                               SharedSessionContractImplementor session,
                               Object owner) throws SQLException {
        float[] vec = rs.getObject(position, float[].class);
        return vec != null ? vec : new float[0];
    }

    @Override
    public void nullSafeSet(PreparedStatement st, float[] value, int index,
                            SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, value);
        }
    }

    @Override
    public float[] deepCopy(float[] value) {
        return value != null ? Arrays.copyOf(value, value.length) : null;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) {
        return value;
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) {
        return (float[]) cached;
    }

    @Override
    public float[] replace(float[] detached, float[] managed, Object owner) {
        return deepCopy(detached);
    }
}
