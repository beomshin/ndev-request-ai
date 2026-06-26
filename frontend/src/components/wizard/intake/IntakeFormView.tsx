/**
 * @file IntakeFormView.tsx
 * @description 신규 지불수단 등록 폼을 동적으로 렌더하는 컴포넌트 모음.
 *
 * yaml 스키마(`IntakeForm`)로부터 섹션과 필드를 읽어 `inputType` 에 따라
 * 알맞은 위젯(텍스트, 숫자, boolean, select, multiselect, group)을 자동 생성한다.
 *
 * @remarks
 * - 섹션은 `sections.order` 오름차순으로 정렬되어 표시된다.
 * - 각 섹션은 `<details>` 태그로 감싸져 있으며, 첫 번째 섹션만 기본으로 펼쳐진다.
 * - 필드 값은 `answers` prop으로 외부에서 관리하며, 변경 시 `onChange` 콜백으로 전달한다.
 */

import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  type IntakeField,
  type IntakeForm,
  type IntakeAnswers,
} from "@/lib/intakeForm";

/**
 * 신규 지불수단 등록 폼 최상위 뷰 컴포넌트.
 * yaml 스키마를 받아 섹션별로 그룹핑된 폼을 렌더한다.
 *
 * @param props.schema - yaml에서 파싱된 폼 스키마 (섹션 + 필드 목록)
 * @param props.answers - 현재 필드 값 모음 (key: policyId, value: 사용자 입력값)
 * @param props.onChange - 필드 값이 변경될 때 호출되는 콜백 (새 answers 전체를 전달)
 */
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
  /**
   * 특정 필드 키의 값을 업데이트하는 편의 함수.
   * 기존 answers를 얕은 복사한 뒤 해당 키만 교체한다.
   *
   * @param key - 변경할 필드의 policyId
   * @param value - 새 값
   */
  const setVal = (key: string, value: unknown) =>
    onChange({ ...answers, [key]: value });

  // 섹션을 order 오름차순으로 정렬
  const sectionOrder = [...schema.sections].sort((a, b) => a.order - b.order);

  // 필드를 섹션 코드별로 그룹핑 (섹션 없는 필드는 "MISC" 버킷으로)
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
        // 해당 섹션에 필드가 없으면 렌더 생략
        if (items.length === 0) return null;
        return (
          // 첫 번째 섹션(idx === 0)만 기본으로 open, 나머지는 접힌 상태
          <details
            key={s.code}
            open={idx === 0}
            className="rounded-md border border-border bg-card"
          >
            <summary className="cursor-pointer px-4 py-3 text-sm font-semibold text-foreground flex items-center justify-between">
              <span>
                {/* 섹션 순서 번호를 모노스페이스 소문자로 표시 */}
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

/**
 * 단일 필드 한 행을 렌더하는 내부 컴포넌트.
 * 라벨, 단위, 실제 입력 위젯, 도움말 텍스트를 수직 배치한다.
 *
 * @param props.field - 렌더할 필드 스키마
 * @param props.value - 현재 필드 값
 * @param props.onChange - 값 변경 콜백
 */
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
  // 접근성용 id — policyId 기반으로 중복 방지
  const id = `f-${field.policyId}`;
  return (
    <div>
      <div className="flex items-center gap-1 mb-1">
        <Label htmlFor={id} className="text-xs">
          {field.label}
          {/* 필수 필드는 빨간 별표 표시 */}
          {field.required && <span className="text-destructive ml-0.5">*</span>}
        </Label>
        {/* 단위가 있으면 괄호 안에 소문자로 표시 */}
        {field.unit && (
          <span className="text-[10px] text-muted-foreground">({field.unit})</span>
        )}
      </div>
      <FieldWidget id={id} field={field} value={value} onChange={onChange} />
      {/* 도움말 텍스트가 있으면 입력창 아래에 작은 글씨로 표시 */}
      {field.helpText && (
        <p className="text-[11px] text-muted-foreground mt-1 leading-snug">
          {field.helpText}
        </p>
      )}
    </div>
  );
}

/**
 * `inputType` 에 따라 알맞은 입력 위젯을 선택해 렌더하는 내부 컴포넌트.
 *
 * @remarks
 * 지원하는 inputType:
 * - `"group"` : 중첩 필드를 객체로 묶어 저장하는 복합 위젯
 * - `"boolean"` : 체크박스 (true/false)
 * - `"select"` : 단일 선택 드롭다운
 * - `"multiselect"` : 복수 선택 칩 버튼
 * - `"number"` : 숫자 입력 (빈값이면 `null` 저장)
 * - `"text"` (기본) : 단일 텍스트 입력
 *
 * @param props.id - 연결된 `<label>` 의 `htmlFor` 값과 일치해야 하는 엘리먼트 id
 * @param props.field - 필드 스키마
 * @param props.value - 현재 값
 * @param props.onChange - 값 변경 콜백
 */
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
  // 현재 값이 없으면 스키마의 defaultValue로 대체
  const effective = value ?? field.defaultValue;

  // group: 중첩 필드를 객체로 묶어 저장
  if (type === "group" && field.fields) {
    // 현재 값을 Record<string, unknown>으로 캐스팅해 중첩 필드 접근
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
            {/* 하위 필드도 재귀적으로 FieldWidget 사용 — 객체 키별 spread 머지 */}
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

  // boolean: 체크박스 — !!effective 로 truthy 변환해 초기값 보장
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

  // select: 단일 선택 드롭다운 — 빈 option을 기본 placeholder로 삽입
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

  // multiselect: 복수 선택 칩 — Set으로 토글 후 Array.from으로 직렬화
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
                // 이미 선택된 항목 클릭 시 제거, 미선택 항목 클릭 시 추가
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

  // number: 숫자 입력 — 빈값이면 null 저장해 0과 구분
  if (type === "number") {
    return (
      <Input
        id={id}
        type="number"
        value={(effective as number | string) ?? ""}
        placeholder={field.placeholder}
        onChange={(e) => {
          const raw = e.target.value;
          // 빈 문자열이면 null 저장, 아니면 숫자로 변환
          onChange(raw === "" ? null : Number(raw));
        }}
      />
    );
  }

  // text (default): 단일 텍스트 입력
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
