import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  type IntakeField,
  type IntakeForm,
  type IntakeAnswers,
} from "@/lib/intakeForm";

// 신규 지불수단 등록 폼을 inputType별 위젯으로 동적 렌더.
// 섹션 단위로 묶어 표시 — yaml의 sections.order 순서를 따른다.
export function IntakeFormView({
  schema,
  answers,
  onChange,
}: {
  schema: IntakeForm;
  answers: IntakeAnswers;
  onChange: (next: IntakeAnswers) => void;
}) {
  const setVal = (key: string, value: unknown) =>
    onChange({ ...answers, [key]: value });

  // 섹션별 그룹핑
  const sectionOrder = [...schema.sections].sort((a, b) => a.order - b.order);
  const byCode = new Map<string, IntakeField[]>();
  for (const f of schema.fields) {
    const code = f.section ?? "MISC";
    if (!byCode.has(code)) byCode.set(code, []);
    byCode.get(code)!.push(f);
  }

  return (
    <div className="space-y-4">
      {sectionOrder.map((s, idx) => {
        const items = byCode.get(s.code) ?? [];
        if (items.length === 0) return null;
        return (
          <details
            key={s.code}
            open={idx === 0}
            className="rounded-md border border-border bg-card"
          >
            <summary className="cursor-pointer px-4 py-3 text-sm font-semibold text-foreground flex items-center justify-between">
              <span>
                <span className="text-xs font-mono text-muted-foreground mr-2">
                  §{s.order}
                </span>
                {s.name}
              </span>
              <span className="text-xs text-muted-foreground font-normal">
                {items.length}개 항목
              </span>
            </summary>
            <div className="px-4 py-3 border-t border-border space-y-4">
              {items.map((f) => (
                <FieldRow
                  key={f.policyId}
                  field={f}
                  value={f.policyId ? answers[f.policyId] : undefined}
                  onChange={(v) => f.policyId && setVal(f.policyId, v)}
                />
              ))}
            </div>
          </details>
        );
      })}
    </div>
  );
}

// 개별 필드 한 줄 — inputType별 위젯 분기
function FieldRow({
  field,
  value,
  onChange,
}: {
  field: IntakeField;
  value: unknown;
  onChange: (v: unknown) => void;
}) {
  const id = `f-${field.policyId}`;
  return (
    <div>
      <div className="flex items-center gap-1 mb-1">
        <Label htmlFor={id} className="text-xs">
          {field.label}
          {field.required && <span className="text-destructive ml-0.5">*</span>}
        </Label>
        {field.unit && (
          <span className="text-[10px] text-muted-foreground">({field.unit})</span>
        )}
      </div>
      <FieldWidget id={id} field={field} value={value} onChange={onChange} />
      {field.helpText && (
        <p className="text-[11px] text-muted-foreground mt-1 leading-snug">
          {field.helpText}
        </p>
      )}
    </div>
  );
}

function FieldWidget({
  id,
  field,
  value,
  onChange,
}: {
  id: string;
  field: IntakeField;
  value: unknown;
  onChange: (v: unknown) => void;
}) {
  const type = field.inputType ?? "text";
  const effective = value ?? field.defaultValue;

  // group: 중첩 필드를 객체로 묶어 저장
  if (type === "group" && field.fields) {
    const obj = (effective as Record<string, unknown>) ?? {};
    return (
      <div className="rounded-md border border-border bg-secondary/30 p-3 space-y-3">
        {field.fields.map((sub) => (
          <div key={sub.key}>
            <Label className="text-[11px] text-muted-foreground">
              {sub.label}
              {sub.unit ? ` (${sub.unit})` : ""}
              {sub.required && <span className="text-destructive ml-0.5">*</span>}
            </Label>
            <FieldWidget
              id={`${id}-${sub.key}`}
              field={sub}
              value={sub.key ? obj[sub.key] : undefined}
              onChange={(v) =>
                sub.key && onChange({ ...obj, [sub.key]: v })
              }
            />
          </div>
        ))}
      </div>
    );
  }

  if (type === "boolean") {
    return (
      <label className="inline-flex items-center gap-2 cursor-pointer">
        <Checkbox
          id={id}
          checked={!!effective}
          onCheckedChange={(v) => onChange(v === true)}
        />
        <span className="text-xs text-muted-foreground">
          {effective ? "예 (true)" : "아니오 (false)"}
        </span>
      </label>
    );
  }

  if (type === "select") {
    return (
      <select
        id={id}
        value={(effective as string) ?? ""}
        onChange={(e) => onChange(e.target.value)}
        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
      >
        <option value="">선택…</option>
        {(field.options ?? []).map((opt) => (
          <option key={opt} value={opt}>
            {opt}
          </option>
        ))}
      </select>
    );
  }

  if (type === "multiselect") {
    const selected = new Set((value as string[]) ?? []);
    return (
      <div className="flex flex-wrap gap-1.5">
        {(field.options ?? []).map((opt) => {
          const on = selected.has(opt);
          return (
            <button
              key={opt}
              type="button"
              onClick={() => {
                const next = new Set(selected);
                if (on) next.delete(opt);
                else next.add(opt);
                onChange(Array.from(next));
              }}
              className={`text-xs rounded-full border px-2.5 py-1 transition ${
                on
                  ? "bg-primary text-primary-foreground border-primary"
                  : "bg-card text-foreground border-border hover:border-primary/40"
              }`}
            >
              {opt}
            </button>
          );
        })}
      </div>
    );
  }

  if (type === "number") {
    return (
      <Input
        id={id}
        type="number"
        value={(effective as number | string) ?? ""}
        placeholder={field.placeholder}
        onChange={(e) => {
          const raw = e.target.value;
          onChange(raw === "" ? null : Number(raw));
        }}
      />
    );
  }

  // text (default)
  return (
    <Input
      id={id}
      type="text"
      value={(effective as string) ?? ""}
      placeholder={field.placeholder}
      maxLength={field.maxLength}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}
