/**
 * @file FileAttachZone.tsx
 * @description 파일 드래그&드롭 / 클릭 업로드 영역 컴포넌트.
 *
 * Phase 0에서는 실제 서버 업로드 없이 파일 이름과 크기만 위저드 전역 상태에 저장한다.
 * 추후 백엔드 파일 업로드 엔드포인트가 확정되면 `handleFiles` 내부에서 fetch를 추가하면 된다.
 *
 * @remarks
 * - 드래그 오버 시 테두리 색상이 변경되어 드롭 가능 영역임을 시각적으로 표시한다.
 * - 동일 이름의 파일은 `addAttachment` reducer에서 중복 처리 없이 그대로 추가된다.
 *   필요 시 `handleFiles` 에서 중복 필터링을 추가할 수 있다.
 * - 첨부 파일 목록은 위저드 제출 시 `details` JSON 안에 포함된다.
 */

import { useRef, useState, type DragEvent } from "react";
import { useWizard } from "../WizardContext";

/**
 * 파일 드래그&드롭 및 클릭 선택 업로드 영역 컴포넌트.
 * 선택한 파일의 메타데이터(이름, 크기)를 위저드 전역 상태 `attachments` 배열에 저장한다.
 *
 * @example
 * // 슬라이드 내부에서 사용
 * <FileAttachZone />
 */
// 드래그&드롭 자리. 실제 업로드는 백엔드 엔드포인트 미정 상태라 메타만 보관.
// 추후 서버 업로드 추가 시 onSelect에서 fetch만 끼우면 됨.
export function FileAttachZone() {
  const { state, addAttachment, removeAttachment } = useWizard();
  // 숨겨진 <input type="file">을 클릭 이벤트로 트리거하기 위한 ref
  const inputRef = useRef<HTMLInputElement>(null);
  // 드래그 오버 상태 — 시각적 피드백(테두리 색상 변경)에 사용
  const [dragOver, setDragOver] = useState(false);

  // 전역 상태에서 현재 첨부 파일 목록 읽기
  const files = state.data.attachments ?? [];

  /**
   * `FileList` 에서 각 파일의 메타데이터를 추출해 전역 상태에 추가한다.
   * Phase 0: 실제 업로드 없이 이름·크기만 저장.
   *
   * @param list - 사용자가 선택하거나 드롭한 FileList (null이면 아무것도 하지 않음)
   */
  const handleFiles = (list: FileList | null) => {
    if (!list) return;
    // 여러 파일 선택 시 각각 별도 항목으로 추가
    Array.from(list).forEach((f) => addAttachment({ name: f.name, size: f.size }));
  };

  /**
   * 드롭 이벤트 핸들러.
   * 브라우저 기본 동작(파일 열기)을 막고 드롭된 파일을 처리한다.
   *
   * @param e - React DragEvent
   */
  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    // 드래그 오버 상태 초기화
    setDragOver(false);
    handleFiles(e.dataTransfer.files);
  };

  return (
    <div>
      {/* 드롭 영역 — 드래그 오버 시 primary 색상 테두리/배경 적용 */}
      <div
        onDragOver={(e) => {
          e.preventDefault(); // 브라우저 기본 동작 차단 (드롭 허용을 위해 필수)
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={onDrop}
        onClick={() => inputRef.current?.click()} // 클릭 시 숨겨진 input 트리거
        className={`rounded-md border border-dashed p-4 text-center cursor-pointer transition ${
          dragOver
            ? "border-primary bg-primary/5"
            : "border-border bg-secondary/20 hover:border-primary/40"
        }`}
      >
        <div className="text-xs text-muted-foreground">
          연동 규격서 파일을 드래그하거나 클릭해 업로드 (PDF / DOCX / MD)
        </div>
        <div className="mt-1 text-[10px] text-muted-foreground/70">
          ※ Phase 0은 파일명만 기록합니다. 실제 업로드는 추후 보강.
        </div>
        {/* 실제 파일 선택 input — 시각적으로 숨김, 위 영역 클릭 시 간접 트리거 */}
        <input
          ref={inputRef}
          type="file"
          multiple
          className="hidden"
          onChange={(e) => handleFiles(e.target.files)}
        />
      </div>

      {/* 첨부 파일 목록 — 파일이 1개 이상일 때만 렌더 */}
      {files.length > 0 && (
        <ul className="mt-2 space-y-1">
          {files.map((f) => (
            <li
              key={f.name}
              className="flex items-center justify-between rounded border border-border bg-background px-3 py-1.5 text-xs"
            >
              {/* 파일 이름 + KB 단위 크기 표시 — 긴 이름은 truncate */}
              <span className="truncate">
                <span className="text-muted-foreground">📎</span> {f.name}{" "}
                <span className="text-muted-foreground">({Math.round(f.size / 1024)} KB)</span>
              </span>
              {/* 개별 파일 제거 버튼 — 클릭 이벤트가 드롭 영역으로 버블링되지 않도록 stopPropagation */}
              <button
                type="button"
                className="text-muted-foreground hover:text-destructive"
                onClick={(e) => {
                  e.stopPropagation(); // 드롭 영역 클릭 이벤트로 버블링 방지
                  removeAttachment(f.name);
                }}
              >
                ✕
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
