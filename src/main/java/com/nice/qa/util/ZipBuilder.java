package com.nice.qa.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 인메모리 ZIP 파일 생성 유틸리티.
 *
 * <p>디스크 I/O 없이 {@link ByteArrayOutputStream}을 백엔드로 사용하여
 * ZIP 아카이브를 메모리에서 직접 조립한다. 생성된 ZIP 바이트 배열은
 * HTTP 응답 스트림에 바로 쓰거나 파일로 저장하는 데 모두 활용할 수 있다.
 *
 * <p>인스턴스화를 방지하기 위해 private 생성자와 final 클래스 선언을 사용한다.
 *
 * @see ZipOutputStream
 */
public final class ZipBuilder {

    /**
     * 유틸리티 클래스이므로 인스턴스화를 방지한다.
     */
    private ZipBuilder() {}

    /**
     * 마크다운 문서와 PNG 이미지를 하나의 ZIP 아카이브로 패키징한다.
     *
     * <p>ZIP 내부 구조:
     * <pre>
     * archive.zip
     * ├── requirements.md   ← 개발요청서 표준 양식 마크다운 (UTF-8 인코딩)
     * └── flow.png          ← 시스템 흐름 다이어그램 PNG 이미지
     * </pre>
     *
     * <p>전체 처리가 메모리 내에서 이루어지므로 파일 시스템 권한이나
     * 임시 파일 관리가 불필요하다.
     *
     * @param markdown {@code requirements.md}에 담길 마크다운 문자열 (UTF-8로 인코딩됨)
     * @param flowPng  {@code flow.png}에 담길 PNG 이미지 바이트 배열
     * @return 완성된 ZIP 아카이브 바이트 배열
     * @throws RuntimeException 스트림 쓰기 또는 ZIP 항목 추가 중 {@link IOException} 발생 시
     */
    public static byte[] buildDevRequestZip(String markdown, byte[] flowPng) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(out)) {

            // requirements.md 항목 추가: 마크다운을 UTF-8 바이트로 변환하여 기록
            zos.putNextEntry(new ZipEntry("requirements.md"));
            zos.write(markdown.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // flow.png 항목 추가: PNG 바이트를 그대로 기록
            zos.putNextEntry(new ZipEntry("flow.png"));
            zos.write(flowPng);
            zos.closeEntry();

            // ZIP 스트림을 마무리(finish)하여 central directory 헤더를 기록
            zos.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("ZIP 생성 실패", e);
        }
    }
}
