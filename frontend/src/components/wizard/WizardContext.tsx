/**
 * @file WizardContext.tsx
 * @description 위저드 전역 상태를 관리하는 React Context 및 Provider.
 *
 * `useReducer` 기반으로 슬라이드 탐색, 데이터 패치, 추가 확인 항목 관리,
 * 파일 첨부 메타 관리 등 위저드의 모든 상태 변경을 처리한다.
 *
 * @example
 * // Provider로 감싼 뒤 자식 컴포넌트에서 훅으로 꺼내 사용
 * <WizardProvider>
 *   <WizardInner />
 * </WizardProvider>
 *
 * // 자식 컴포넌트 내부
 * const { state, next, patch } = useWizard();
 */

import { createContext, useContext, useReducer, type ReactNode } from "react";
import {
  TOTAL_SLIDES,
  type AdditionalCheckItem,
  type AttachmentPlaceholder,
  type WizardAction,
  type WizardData,
  type WizardState,
} from "./types";

/**
 * 위저드 초기 상태.
 * `createdAt`은 S3 표시용으로 미리 오늘 날짜로 설정하고,
 * 나머지 수집 필드는 빈 값으로 시작한다.
 */
const initialState: WizardState = {
  currentSlide: 1,
  direction: 1,
  data: {
    // 요청서 생성일 — S3 자동 표시 및 최종 저장 payload에 포함
    createdAt: new Date().toISOString().slice(0, 10),
    additionalCheckItems: [],
    attachments: [],
    s6: {},
  },
};

/**
 * 위저드 상태 reducer.
 * 모든 상태 변경은 이 함수를 통해 불변(immutable)하게 처리된다.
 *
 * @param state - 현재 위저드 상태
 * @param action - 처리할 액션
 * @returns 새 위저드 상태
 */
function reducer(state: WizardState, action: WizardAction): WizardState {
  switch (action.type) {
    case "GOTO": {
      // 지정한 슬라이드로 직접 이동 — 범위를 벗어나지 않도록 clamp 처리
      const next = clamp(action.slide, 1, TOTAL_SLIDES);
      return { ...state, currentSlide: next, direction: next >= state.currentSlide ? 1 : -1 };
    }
    case "NEXT": {
      // 다음 슬라이드로 이동 — 방향을 1(앞)로 설정
      const next = clamp(state.currentSlide + 1, 1, TOTAL_SLIDES);
      return { ...state, currentSlide: next, direction: 1 };
    }
    case "PREV": {
      // 이전 슬라이드로 이동 — 방향을 -1(뒤)로 설정
      const next = clamp(state.currentSlide - 1, 1, TOTAL_SLIDES);
      return { ...state, currentSlide: next, direction: -1 };
    }
    case "PATCH": {
      // 데이터 부분 병합 — s6는 깊은 머지 적용 (mergeData 참고)
      return { ...state, data: mergeData(state.data, action.patch) };
    }
    case "ADD_CHECK": {
      // 동일 field가 이미 존재하면 중복 추가하지 않고 현재 상태 그대로 반환
      const exists = state.data.additionalCheckItems?.some(
        (it) => it.field === action.item.field,
      );
      if (exists) return state;
      return {
        ...state,
        data: {
          ...state.data,
          additionalCheckItems: [...(state.data.additionalCheckItems ?? []), action.item],
        },
      };
    }
    case "REMOVE_CHECK": {
      // field 이름으로 추가 확인 항목을 배열에서 제거
      return {
        ...state,
        data: {
          ...state.data,
          additionalCheckItems: (state.data.additionalCheckItems ?? []).filter(
            (it) => it.field !== action.field,
          ),
        },
      };
    }
    case "ADD_ATTACHMENT": {
      // 파일 첨부 플레이스홀더를 목록에 추가
      return {
        ...state,
        data: {
          ...state.data,
          attachments: [...(state.data.attachments ?? []), action.file],
        },
      };
    }
    case "REMOVE_ATTACHMENT": {
      // 파일 이름으로 첨부 항목을 목록에서 제거
      return {
        ...state,
        data: {
          ...state.data,
          attachments: (state.data.attachments ?? []).filter((f) => f.name !== action.name),
        },
      };
    }
    default:
      return state;
  }
}

/**
 * 위저드 데이터를 얕은 머지로 병합하되, `s6` 필드는 한 단계 더 깊게 머지한다.
 *
 * @remarks
 * `s6.cardOptions` 가 patch에 일부만 포함되어 있을 때 기존 값이 통째로 덮이는 것을
 * 방지하기 위해 `cardOptions` 레벨까지 명시적으로 병합한다.
 *
 * @param prev - 현재 데이터
 * @param patch - 덮어쓸 부분 데이터
 * @returns 병합된 새 데이터 객체
 */
function mergeData(prev: WizardData, patch: WizardData): WizardData {
  // 1단계: 최상위 얕은 머지
  const merged: WizardData = { ...prev, ...patch };
  if (patch.s6) {
    // 2단계: s6는 기존 값에 patch.s6를 얕게 덮어쓰고
    // cardOptions는 한 단계 더 깊게 머지하여 기존 옵션을 보존
    merged.s6 = {
      ...(prev.s6 ?? {}),
      ...patch.s6,
      cardOptions: patch.s6.cardOptions
        ? { ...(prev.s6?.cardOptions ?? {}), ...patch.s6.cardOptions }
        : prev.s6?.cardOptions,
    };
  }
  return merged;
}

/**
 * 숫자 `n`을 `[min, max]` 범위로 제한한다.
 *
 * @param n - 제한할 값
 * @param min - 최솟값 (포함)
 * @param max - 최댓값 (포함)
 * @returns 범위 내로 클램프된 값
 */
function clamp(n: number, min: number, max: number) {
  return Math.max(min, Math.min(max, n));
}

// ─────────────────────────────────────────────────────────────────────
// Context
// ─────────────────────────────────────────────────────────────────────

/**
 * WizardContext 값의 타입 정의.
 * `useWizard()` 훅이 반환하는 값의 형태다.
 */
type WizardCtx = {
  /** 현재 위저드 상태 (읽기 전용으로 사용) */
  state: WizardState;
  /**
   * 특정 슬라이드로 직접 이동한다.
   * @param slide - 이동할 슬라이드 번호 (1-based, 범위 초과 시 clamp)
   */
  goto: (slide: number) => void;
  /** 다음 슬라이드로 이동한다. */
  next: () => void;
  /** 이전 슬라이드로 이동한다. */
  prev: () => void;
  /**
   * 위저드 데이터를 부분 병합한다.
   * @param patch - 덮어쓸 데이터 (partial)
   */
  patch: (patch: WizardData) => void;
  /**
   * 추가 확인 항목을 추가한다. 동일 `field`가 이미 있으면 무시한다.
   * @param item - 추가할 항목
   */
  addCheck: (item: AdditionalCheckItem) => void;
  /**
   * 특정 필드의 추가 확인 항목을 제거한다.
   * @param field - 제거할 항목의 field 라벨
   */
  removeCheck: (field: string) => void;
  /**
   * 파일 첨부 플레이스홀더를 추가한다.
   * @param file - 추가할 파일 메타 객체
   */
  addAttachment: (file: AttachmentPlaceholder) => void;
  /**
   * 특정 이름의 첨부 파일을 제거한다.
   * @param name - 제거할 파일 이름
   */
  removeAttachment: (name: string) => void;
};

/** 위저드 React Context 인스턴스. 초기값은 `null` 이며 Provider 내부에서만 유효하다. */
const WizardContext = createContext<WizardCtx | null>(null);

/**
 * 위저드 전역 상태 Provider.
 * `WizardShell` 이 이 컴포넌트로 `WizardInner` 를 감싸며,
 * 하위 컴포넌트는 `useWizard()` 훅으로 상태에 접근한다.
 *
 * @param props.children - Provider로 감쌀 자식 노드
 */
export function WizardProvider({ children }: { children: ReactNode }) {
  // useReducer로 위저드 상태와 dispatcher를 생성
  const [state, dispatch] = useReducer(reducer, initialState);

  // dispatcher를 래핑한 편의 함수들을 context value로 제공
  const value: WizardCtx = {
    state,
    goto: (slide) => dispatch({ type: "GOTO", slide }),
    next: () => dispatch({ type: "NEXT" }),
    prev: () => dispatch({ type: "PREV" }),
    patch: (patch) => dispatch({ type: "PATCH", patch }),
    addCheck: (item) => dispatch({ type: "ADD_CHECK", item }),
    removeCheck: (field) => dispatch({ type: "REMOVE_CHECK", field }),
    addAttachment: (file) => dispatch({ type: "ADD_ATTACHMENT", file }),
    removeAttachment: (name) => dispatch({ type: "REMOVE_ATTACHMENT", name }),
  };

  return <WizardContext.Provider value={value}>{children}</WizardContext.Provider>;
}

/**
 * 위저드 상태에 접근하는 커스텀 훅.
 * 반드시 `<WizardProvider>` 하위 트리 안에서 호출해야 한다.
 *
 * @returns `WizardCtx` — 상태와 상태 변경 함수 묶음
 * @throws `<WizardProvider>` 외부에서 호출하면 에러를 던진다.
 *
 * @example
 * const { state, next, patch } = useWizard();
 */
export function useWizard() {
  const ctx = useContext(WizardContext);
  if (!ctx) throw new Error("useWizard()는 <WizardProvider> 안에서만 사용 가능");
  return ctx;
}
