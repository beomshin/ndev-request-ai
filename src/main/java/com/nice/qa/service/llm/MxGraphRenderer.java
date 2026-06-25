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
 * mxGraph(JGraphX) XML을 PNG 바이트 배열로 렌더링하는 컴포넌트.
 *
 * <p>LLM이 생성한 draw.io 호환 {@code mxGraphModel} XML을 서버 사이드에서 이미지로 변환한다.
 * 클라이언트(브라우저)가 draw.io를 사용할 수 없는 환경(예: 문서 첨부, 이메일 전송)에서
 * 다이어그램 이미지가 필요할 때 사용된다.
 *
 * <p>시퀀스 다이어그램은 각 셀의 좌표가 시간 순서라는 의미를 가지므로,
 * JGraphX의 자동 레이아웃({@code mxHierarchicalLayout} 등)을 적용하지 않고
 * XML에 박힌 좌표를 그대로 사용하여 렌더링한다.
 *
 * <p>렌더링 배율({@link #SCALE})은 2.0으로 설정되어 있어 고해상도(Retina) 환경에서도
 * 선명한 이미지를 제공한다.
 *
 * <p>렌더링 실패(XML 파싱 오류, 빈 그래프 등)는 {@link DiagramRenderException}으로
 * 래핑되어 서비스 계층에 전파된다.
 *
 * @see DiagramPromptBuilder  LLM에게 mxGraph XML 생성을 지시하는 프롬프트 빌더
 */
@Slf4j
@Component
public class MxGraphRenderer {

    /**
     * PNG 렌더링 배율.
     * 2.0 = 논리 해상도의 2배 크기로 렌더링하여 고해상도 디스플레이 대응.
     * 배율이 높을수록 파일 크기가 커지므로 필요에 따라 조정 가능.
     */
    private static final double SCALE = 2.0;

    /**
     * mxGraph XML 문자열을 PNG 이미지 바이트 배열로 변환한다.
     *
     * <p>처리 순서:
     * <ol>
     *   <li>XML 파싱 및 {@link mxGraph} 모델 구성 ({@link #buildGraph(String)})</li>
     *   <li>{@link mxCellRenderer#createBufferedImage} 로 {@link BufferedImage} 생성
     *       — 배경색 흰색, 자동 레이아웃 없음, 배율 {@link #SCALE}</li>
     *   <li>렌더링 결과가 {@code null}인 경우(빈 그래프, 셀 없음) 예외 발생</li>
     *   <li>{@link ImageIO#write} 로 PNG 바이트로 인코딩하여 반환</li>
     * </ol>
     *
     * <p>소요 시간은 로그로 기록된다.
     *
     * @param xml LLM이 생성한 {@code mxGraphModel} XML 문자열.
     *            {@code <mxGraphModel>}으로 시작하고 {@code </mxGraphModel>}으로 끝나야 한다.
     *            코드펜스가 포함된 경우 {@link LlmResponseParser#stripCodeFence(String)}로
     *            먼저 제거해야 한다.
     * @return PNG 포맷의 이미지 바이트 배열. HTTP 응답 바디 또는 파일 저장에 직접 사용 가능.
     * @throws DiagramRenderException XML 파싱 실패, 빈 그래프, ImageIO 쓰기 실패 등 렌더링 오류 발생 시.
     */
    public byte[] toPng(String xml) {
        log.info("[Renderer] mxGraph PNG 렌더링 시작");
        long start = System.currentTimeMillis();
        try {
            // 1단계: XML → mxGraph 모델 객체로 변환
            mxGraph graph = buildGraph(xml);

            // 2단계: mxGraph 모델을 BufferedImage로 래스터 렌더링
            // 인수: (그래프, 선택셀, 배율, 배경색, 클리핑여부, 이미지옵저버)
            // 배경색 Color.WHITE → 흰색 배경, null 이면 투명 배경
            BufferedImage image = mxCellRenderer.createBufferedImage(
                    graph, null, SCALE, Color.WHITE, true, null);

            // 렌더링 결과가 null인 경우 → XML에 표시 가능한 셀이 없거나 좌표가 비정상
            if (image == null) {
                log.warn("[Renderer] 렌더링 결과 이미지가 비었습니다 (xml 길이={})", xml.length());
                throw new DiagramRenderException("렌더링 결과 이미지가 비었습니다", null);
            }

            // 3단계: BufferedImage → PNG 바이트 배열로 인코딩
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            long elapsed = System.currentTimeMillis() - start;
            log.info("[Renderer] PNG 렌더링 완료 ({}ms, {}bytes)", elapsed, out.size());
            return out.toByteArray();

        } catch (DiagramRenderException e) {
            // 이미 래핑된 예외는 재래핑 없이 그대로 전파
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[Renderer] PNG 렌더링 실패 ({}ms)", elapsed, e);
            // XML 파싱 오류, ImageIO 오류 등 기타 예외를 DiagramRenderException으로 래핑
            throw new DiagramRenderException("mxGraph PNG 렌더링 실패", e);
        }
    }

    /**
     * mxGraph XML 문자열을 파싱하여 {@link mxGraph} 객체를 구성한다.
     *
     * <p>JGraphX 디코딩 절차:
     * <ol>
     *   <li>{@link mxXmlUtils#parseXml(String)} 으로 XML 문자열 → DOM {@link Document} 변환</li>
     *   <li>{@link mxCodec} 인스턴스 생성 — 동일한 DOM 문서를 참조해야 올바른 디코딩 가능</li>
     *   <li>{@code mxCodec.decode()} 로 DOM 루트 요소를 {@code mxGraph} 모델에 적용</li>
     * </ol>
     *
     * <p>{@code beginUpdate()} / {@code endUpdate()} 쌍으로 모델 변경을 트랜잭션 처리하여
     * 모든 셀이 원자적으로 추가되도록 한다. {@code finally} 블록에서 {@code endUpdate()}를
     * 보장하므로 파싱 예외가 발생해도 모델이 잠긴 상태로 남지 않는다.
     *
     * <p>시퀀스 다이어그램 특성상 좌표가 의미를 가지므로 자동 레이아웃은 적용하지 않는다.
     *
     * @param xml {@code mxGraphModel} XML 문자열
     * @return 파싱된 셀 정보가 채워진 {@link mxGraph} 객체
     * @throws Exception XML 파싱 오류 또는 mxCodec 디코딩 오류 발생 시
     */
    private mxGraph buildGraph(String xml) {
        mxGraph graph = new mxGraph();
        // 모델 수정 시작 — 배치 업데이트로 렌더링 성능 최적화 및 원자적 상태 보장
        graph.getModel().beginUpdate();
        try {
            // XML 문자열 → W3C DOM Document 파싱
            Document doc = mxXmlUtils.parseXml(xml);
            // mxCodec: JGraphX의 XML 직렬화/역직렬화 코덱
            // DOM 루트 요소(mxGraphModel)를 graph.getModel()에 디코딩하여 셀 구조 복원
            new mxCodec(doc).decode(doc.getDocumentElement(), graph.getModel());
        } finally {
            // 예외 발생 여부와 무관하게 반드시 endUpdate() 호출
            // 누락 시 그래프 모델이 영구적으로 잠겨 렌더링 불가
            graph.getModel().endUpdate();
        }
        return graph;
    }
}
