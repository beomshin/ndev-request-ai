import { useRef, useState, type DragEvent } from "react";
import { useWizard } from "../WizardContext";

// 드래그&드롭 자리. 실제 업로드는 백엔드 엔드포인트 미정 상태라 메타만 보관.
// 추후 서버 업로드 추가 시 onSelect에서 fetch만 끼우면 됨.
export function FileAttachZone() {
  const { state, addAttachment, removeAttachment } = useWizard();
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragOver, setDragOver] = useState(false);

  const files = state.data.attachments ?? [];

  const handleFiles = (list: FileList | null) => {
    if (!list) return;
    Array.from(list).forEach((f) => addAttachment({ name: f.name, size: f.size }));
  };

  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragOver(false);
    handleFiles(e.dataTransfer.files);
  };

  return (
    <div>
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={onDrop}
        onClick={() => inputRef.current?.click()}
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
        <input
          ref={inputRef}
          type="file"
          multiple
          className="hidden"
          onChange={(e) => handleFiles(e.target.files)}
        />
      </div>

      {files.length > 0 && (
        <ul className="mt-2 space-y-1">
          {files.map((f) => (
            <li
              key={f.name}
              className="flex items-center justify-between rounded border border-border bg-background px-3 py-1.5 text-xs"
            >
              <span className="truncate">
                <span className="text-muted-foreground">📎</span> {f.name}{" "}
                <span className="text-muted-foreground">({Math.round(f.size / 1024)} KB)</span>
              </span>
              <button
                type="button"
                className="text-muted-foreground hover:text-destructive"
                onClick={(e) => {
                  e.stopPropagation();
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
