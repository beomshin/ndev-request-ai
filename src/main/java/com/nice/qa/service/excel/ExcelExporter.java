package com.nice.qa.service.excel;

import com.nice.qa.dto.TestCaseDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

// 케이스 목록을 xlsx 바이트로 변환. Apache POI(XSSF) 사용.
@Component
public class ExcelExporter {

    private static final String SHEET_NAME = "QA_테스트케이스";
    private static final String[] HEADERS = {
            "번호", "결제수단", "인증방식", "할부", "케이스분류", "테스트내용", "예상결과", "근거"
    };
    // 컬럼별 너비(글자수 기준). 마지막에 *256 해서 POI 단위로 적용.
    private static final int[] COLUMN_WIDTHS = {6, 14, 14, 10, 12, 50, 40, 40};

    public byte[] export(List<TestCaseDto> cases) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet(SHEET_NAME);

            // 컬럼 너비 적용
            for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
                sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
            }

            // 헤더 스타일: 볼드 + 배경색(회색) + 가운데정렬 + 테두리
            CellStyle headerStyle = buildHeaderStyle(wb);
            CellStyle bodyStyle = buildBodyStyle(wb, IndexedColors.WHITE);
            CellStyle normalStyle = buildBodyStyle(wb, new Color(220, 245, 220)); // 연초록
            CellStyle exceptionStyle = buildBodyStyle(wb, new Color(255, 220, 220)); // 연빨강
            CellStyle boundaryStyle = buildBodyStyle(wb, new Color(255, 235, 200)); // 연주황

            // 헤더 행
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(22);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 본문 행
            int rowIdx = 1;
            for (TestCaseDto c : cases) {
                Row row = sheet.createRow(rowIdx);
                CellStyle styleForRow = pickStyle(c.category(), normalStyle, exceptionStyle, boundaryStyle, bodyStyle);

                writeCell(row, 0, String.valueOf(rowIdx), styleForRow);
                writeCell(row, 1, nullToEmpty(c.paymentMethod()), styleForRow);
                writeCell(row, 2, nullToEmpty(c.authMethod()), styleForRow);
                writeCell(row, 3, nullToEmpty(c.installment()), styleForRow);
                writeCell(row, 4, nullToEmpty(c.category()), styleForRow);
                writeCell(row, 5, nullToEmpty(c.content()), styleForRow);
                writeCell(row, 6, nullToEmpty(c.expected()), styleForRow);
                writeCell(row, 7, nullToEmpty(c.rationale()), styleForRow);
                rowIdx++;
            }

            // 첫 행 freeze
            sheet.createFreezePane(0, 1);

            // 헤더 행 자동필터(보기 편하게)
            if (rowIdx > 1) {
                sheet.setAutoFilter(new CellRangeAddress(0, rowIdx - 1, 0, HEADERS.length - 1));
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("xlsx 생성 실패", e);
        }
    }

    // 케이스분류 값에 따라 본문 스타일 선택
    private CellStyle pickStyle(String category, CellStyle normal, CellStyle exception, CellStyle boundary, CellStyle fallback) {
        if (category == null) return fallback;
        return switch (category.trim()) {
            case "정상" -> normal;
            case "예외" -> exception;
            case "경계" -> boundary;
            default -> fallback;
        };
    }

    private void writeCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        // 헤더 배경: 진한 회색
        XSSFColor headerBg = new XSSFColor(new Color(68, 114, 196), null);
        ((org.apache.poi.xssf.usermodel.XSSFCellStyle) style).setFillForegroundColor(headerBg);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorder(style);
        return style;
    }

    private CellStyle buildBodyStyle(Workbook wb, IndexedColors bg) {
        CellStyle style = wb.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        if (bg != null && bg != IndexedColors.WHITE) {
            style.setFillForegroundColor(bg.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        applyBorder(style);
        return style;
    }

    // AWT Color 기반 본문 스타일(연초록/연빨강/연주황 같은 커스텀 색용)
    private CellStyle buildBodyStyle(Workbook wb, Color color) {
        CellStyle style = wb.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        XSSFColor xc = new XSSFColor(color, null);
        ((org.apache.poi.xssf.usermodel.XSSFCellStyle) style).setFillForegroundColor(xc);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(style);
        return style;
    }

    private void applyBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
