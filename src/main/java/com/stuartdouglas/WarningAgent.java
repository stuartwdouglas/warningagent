package com.stuartdouglas;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

public class WarningAgent {

    static Timer t = new Timer();

    public static final Set<String> USED = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public static void add(String name) {
        USED.add(name);
    }

    static final AtomicInteger count = new AtomicInteger();

    public static void premain(java.lang.String s, final java.lang.instrument.Instrumentation i) {

        i.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (className.equals("jdk.internal.module.IllegalAccessLogger")) {
                    try {
                        ClassFile classFile = new ClassFile(new DataInputStream(new ByteArrayInputStream(classfileBuffer)));
                        for (MethodInfo method : classFile.getMethods()) {
                            if (method.getName().startsWith("log") && method.getDescriptor().endsWith("V")) {
                                Bytecode bc = new Bytecode(classFile.getConstPool());
                                bc.addOpcode(Opcode.RETURN);
                                method.getCodeAttribute().set(bc.toCodeAttribute().getCode());
                            }
                        }
                        ByteArrayOutputStream ba = new ByteArrayOutputStream();

                        classFile.write(new DataOutputStream(ba));
                        return ba.toByteArray();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return classfileBuffer;

            }
        });
    }
}
