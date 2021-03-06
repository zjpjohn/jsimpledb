
/*
 * Copyright (C) 2014 Archie L. Cobbs. All rights reserved.
 *
 * $Id$
 */

package org.jsimpledb;

import java.lang.reflect.Method;

import org.jsimpledb.schema.CounterSchemaField;
import org.objectweb.asm.ClassWriter;

/**
 * Represents a counter field in a {@link JClass}.
 *
 * @see org.jsimpledb.Counter
 */
public class JCounterField extends JField {

    JCounterField(JSimpleDB jdb, String name, int storageId, String description, Method getter) {
        super(jdb, name, storageId, description, getter);
    }

    @Override
    public Counter getValue(JObject jobj) {
        if (jobj == null)
            throw new IllegalArgumentException("null jobj");
        return jobj.getTransaction().readCounterField(jobj, this.storageId, false);
    }

    @Override
    public <R> R visit(JFieldSwitch<R> target) {
        return target.caseJCounterField(this);
    }

    @Override
    CounterSchemaField toSchemaItem(JSimpleDB jdb) {
        final CounterSchemaField schemaField = new CounterSchemaField();
        this.initialize(jdb, schemaField);
        return schemaField;
    }

    @Override
    void outputMethods(final ClassGenerator<?> generator, ClassWriter cw) {
        this.outputReadMethod(generator, cw, ClassGenerator.READ_COUNTER_FIELD_METHOD);
    }

    @Override
    JCounterFieldInfo toJFieldInfo() {
        return new JCounterFieldInfo(this);
    }
}

