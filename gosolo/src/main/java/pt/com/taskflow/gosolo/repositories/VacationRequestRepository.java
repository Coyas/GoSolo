package pt.com.taskflow.gosolo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.com.taskflow.gosolo.models.VacationRequest;

import java.time.LocalDate;
import java.util.List;

public interface VacationRequestRepository extends JpaRepository<VacationRequest, Long> {

    List<VacationRequest> findByUserId(Long userId);

    List<VacationRequest> findByUserManagerId(Long managerId);

    // manager vê as suas próprias férias + as dos seus colaboradores numa só query
    // (OR)
    @Query("SELECT v FROM VacationRequest v WHERE v.user.manager.id = :managerId OR v.user.id = :userId")
    List<VacationRequest> findByUserManagerIdOrUserId(
            @Param("managerId") Long managerId,
            @Param("userId") Long userId);

    /*
     * verifica se existe algum pedido do utilizador que se sobreponha às datas do
     * novo pedido e que não esteja rejeitado — usado para bloquear criação de
     * pedidos que ta sobrepoi outros ativos (pendentes ou aprovados). O update tem
     * uma
     * query semelhante que exclui o próprio pedido para não bloquear a si mesmo.
     */
    @Query("""
                SELECT COUNT(v) > 0
                FROM VacationRequest v
                WHERE v.user.id = :userId
                AND v.status <> 'REJECTED'
                AND v.startDate <= :endDate
                AND v.endDate >= :startDate
            """)
    boolean existsOverlappingActiveVacation(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // igual ao anterior mas exclui o próprio pedido — usado no update para não
    // bloquear a si mesmo
    @Query("""
                SELECT COUNT(v) > 0
                FROM VacationRequest v
                WHERE v.id != :requestId
                AND v.user.id = :userId
                AND v.status = 'APPROVED'
                AND v.startDate <= :endDate
                AND v.endDate >= :startDate
            """)
    boolean existsOverlappingApprovedVacationExcludingRequest(
            @Param("requestId") Long requestId,
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /*
     * Regra global do PDF — ka pode ter dois colaboradores aprovados no mesmo
     * período,
     * em toda a empresa. Check feito no approve antes de confirmar.
     */
    @Query("""
                SELECT COUNT(v) > 0
                FROM VacationRequest v
                WHERE v.id != :requestId
                AND v.status = 'APPROVED'
                AND v.startDate <= :endDate
                AND v.endDate >= :startDate
            """)
    boolean existsOverlappingApprovedVacationGlobal(
            @Param("requestId") Long requestId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
