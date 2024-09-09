import { useState, useMemo } from 'react';

interface PaginationResult<T> {
    currentPage: number;
    totalPages: number;
    pageSize: number;
    paginatedItems: T[];
    goToPage: (page: number) => void;
    nextPage: () => void;
    prevPage: () => void;
    setPageSize: (size: number) => void;
}

/**
 * Custom hook for client-side pagination.
 *
 * @param items - Full array of items
 * @param initialPageSize - Items per page (default: 10)
 * @returns Pagination controls and current page items
 */
export function usePagination163<T>(
    items: T[],
    initialPageSize: number = 10
): PaginationResult<T> {
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize, setPageSize] = useState(initialPageSize);

    const totalPages = Math.ceil(items.length / pageSize);

    const paginatedItems = useMemo(() => {
        const start = (currentPage - 1) * pageSize;
        return items.slice(start, start + pageSize);
    }, [items, currentPage, pageSize]);

    const goToPage = (page: number) => {
        setCurrentPage(Math.max(1, Math.min(page, totalPages)));
    };

    const nextPage = () => goToPage(currentPage + 1);
    const prevPage = () => goToPage(currentPage - 1);

    return {
        currentPage,
        totalPages,
        pageSize,
        paginatedItems,
        goToPage,
        nextPage,
        prevPage,
        setPageSize,
    };
}

export default usePagination163;
