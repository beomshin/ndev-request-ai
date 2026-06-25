package com.nice.qa.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 인메모리 ZIP 파일 생성 유틸리티.
 */
public final class ZipBuilder {

    private ZipBuilder() {}

    /**
     * 마크다운 문서와 PNG 이미지를 하나의 ZIP으로 패키징한다.
     *
     * @param markdown requirements.md 내용
     * @param flowPng  flow.png 바이트
     * @return ZIP 바이트 배열
     * @throws RuntimeException ZIP 생성 실패 시
     */
    public static byte[] buildDevRequestZip(String markdown, byte[] flowPng) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(out)) {

            zos.putNextEntry(new ZipEntry("requirements.md"));
            zos.write(markdown.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("flow.png"));
            zos.write(flowPng);
            zos.closeEntry();

            zos.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("ZIP 생성 실패", e);
        }
    }
}
