package com.nice.qa.service.llm;

import com.mxgraph.io.mxCodec;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import com.nice.qa.exception.DiagramRenderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * mxGraph XML을 PNG 바이트로 렌더링.
 * 시퀀스 다이어그램은 좌표가 의미를 가지므로 자동 레이아웃 없이 그대로 그린다.
 */
@Slf4j
@Component
public class MxGraphRenderer {

    private static final double SCALE = 2.0;

    /**
     * mxGraph XML을 PNG로 변환한다.
     *
     * @param xml mxGraphModel XML 문자열
     * @return PNG 바이트 배열
     * @throws DiagramRenderException 렌더링 실패 시
     */
    public byte[] toPng(String xml) {
        log.info("[Renderer] mxGraph PNG 렌더링 시작");
        long start = System.currentTimeMillis();
        try {
            mxGraph graph = buildGraph(xml);
            BufferedImage image = mxCellRenderer.createBufferedImage(
                    graph, null, SCALE, Color.WHITE, true, null);

            if (image == null) {
                log.warn("[Renderer] 렌더링 결과 이미지가 비었습니다 (xml 길이={})", xml.length());
                throw new DiagramRenderException("렌더링 결과 이미지가 비었습니다", null);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            long elapsed = System.currentTimeMillis() - start;
            log.info("[Renderer] PNG 렌더링 완료 ({}ms, {}bytes)", elapsed, out.size());
            return out.toByteArray();

        } catch (DiagramRenderException e) {
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[Renderer] PNG 렌더링 실패 ({}ms)", elapsed, e);
            throw new DiagramRenderException("mxGraph PNG 렌더링 실패", e);
        }
    }

    private mxGraph buildGraph(String xml) {
        mxGraph graph = new mxGraph();
        graph.getModel().beginUpdate();
        try {
            Document doc = mxXmlUtils.parseXml(xml);
            new mxCodec(doc).decode(doc.getDocumentElement(), graph.getModel());
        } finally {
            graph.getModel().endUpdate();
        }
        return graph;
    }
}
