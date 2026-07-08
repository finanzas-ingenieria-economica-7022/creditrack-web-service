package com.creditrack.simulation.application;

import com.creditrack.simulation.domain.model.*;
import com.creditrack.simulation.domain.repositories.PaymentScheduleRepository;
import com.creditrack.simulation.domain.repositories.SimulationRepository;
import com.creditrack.simulation.interfaces.rest.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final PaymentScheduleRepository scheduleRepository;
    private final FinancialEngine engine;

    @Transactional
    public SimulationResponse save(SimulationRequest request, Long ownerUserId) {
        ProductParams params = buildProductParams(request);
        FinancialEngine.Input input = buildInput(request, params);
        FinancialCalculationResult result = engine.calculate(input);

        Simulation entity = buildEntity(request, input, result, ownerUserId);
        entity = simulationRepository.save(entity);
        entity.setCode("SIM-%06d".formatted(entity.getId()));
        entity = simulationRepository.save(entity);

        Long simulationId = entity.getId();
        List<PaymentSchedule> periods = result.getSchedule().stream()
            .map(row -> toPeriod(simulationId, row)).toList();
        scheduleRepository.saveAll(periods);
        return toResponse(entity, result.getSchedule());
    }

    @Transactional
    public SimulationResponse update(Long id, SimulationRequest request, Long ownerUserId) {
        Simulation entity = accessible(id, ownerUserId);
        ProductParams params = buildProductParams(request);
        FinancialEngine.Input input = buildInput(request, params);
        FinancialCalculationResult result = engine.calculate(input);

        applyResult(entity, request, input, result);
        entity = simulationRepository.save(entity);

        scheduleRepository.deleteBySimulationId(entity.getId());
        Long simulationId = entity.getId();
        List<PaymentSchedule> periods = result.getSchedule().stream()
            .map(row -> toPeriod(simulationId, row)).toList();
        scheduleRepository.saveAll(periods);
        return toResponse(entity, result.getSchedule());
    }

    @Transactional(readOnly = true)
    public Page<SimulationResponse> findAll(Pageable pageable, Long ownerUserId, boolean isAdmin) {
        Page<Simulation> page = isAdmin
            ? simulationRepository.findByArchivedFalse(pageable)
            : simulationRepository.findByOwnerUserIdAndArchivedFalse(ownerUserId, pageable);
        return page.map(e -> toResponse(e, null));
    }

    @Transactional(readOnly = true)
    public SimulationResponse findById(Long id, Long ownerUserId) {
        Simulation entity = accessible(id, ownerUserId);
        List<PaymentRow> rows = scheduleRepository.findBySimulationIdOrderByPeriod(id)
            .stream().map(this::fromPeriod).toList();
        return toResponse(entity, rows);
    }

    @Transactional
    public void archive(Long id, Long ownerUserId) {
        Simulation entity = accessible(id, ownerUserId);
        entity.setArchived(true);
        entity.setStatus("Archivado");
        simulationRepository.save(entity);
    }

    private Simulation accessible(Long id, Long ownerUserId) {
        return simulationRepository.findByIdAndOwnerUserIdAndArchivedFalse(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("Simulacion no encontrada"));
    }

    private ProductParams buildProductParams(SimulationRequest req) {
        return new DefaultProductParams(
            req.getTeaPercent() != null ? req.getTeaPercent() : new BigDecimal("14"),
            req.getMinTermMonths() != null ? req.getMinTermMonths() : 1,
            req.getMaxTermMonths() != null ? req.getMaxTermMonths() : 84,
            req.getMinDownPaymentPercent() != null ? req.getMinDownPaymentPercent() : BigDecimal.ZERO,
            new BigDecimal("100"),
            req.getBalloonPercent() != null && req.getBalloonPercent().signum() > 0,
            req.getBalloonPercent() != null ? req.getBalloonPercent() : BigDecimal.ZERO,
            req.getCreditLifeInsuranceMonthlyPercent() != null ? req.getCreditLifeInsuranceMonthlyPercent() : BigDecimal.ZERO,
            req.getVehicleInsuranceAnnualPercent() != null ? req.getVehicleInsuranceAnnualPercent() : BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            req.getCurrency() != null ? req.getCurrency() : "PEN"
        );
    }

    private FinancialEngine.Input buildInput(SimulationRequest req, ProductParams params) {
        BigDecimal life = Boolean.TRUE.equals(req.getCreditLifeInsuranceEnabled())
            ? (req.getCreditLifeInsuranceMonthlyPercent() != null ? req.getCreditLifeInsuranceMonthlyPercent() : BigDecimal.ZERO)
            : BigDecimal.ZERO;
        BigDecimal vehicle = Boolean.TRUE.equals(req.getVehicleInsuranceEnabled())
            ? (req.getVehicleInsuranceAnnualPercent() != null ? req.getVehicleInsuranceAnnualPercent() : BigDecimal.ZERO)
            : BigDecimal.ZERO;
        GraceType graceType = req.getGraceType() != null ? req.getGraceType() : GraceType.NONE;
        int graceMonths = req.getGraceMonths() != null ? req.getGraceMonths() : 0;
        return new FinancialEngine.Input(
            req.getVehiclePrice(),
            req.getTeaPercent(),
            req.getDownPaymentPercent(),
            req.getBalloonPercent() != null ? req.getBalloonPercent() : BigDecimal.ZERO,
            req.getCokTeaPercent(),
            req.getTermMonths(),
            graceType,
            graceMonths,
            req.getFirstPaymentDate(),
            req.getPaymentDay() != null ? req.getPaymentDay() : 1,
            life,
            vehicle,
            params
        );
    }

    private Simulation buildEntity(SimulationRequest req, FinancialEngine.Input input,
                                   FinancialCalculationResult result, Long ownerUserId) {
        Simulation e = new Simulation();
        applyResult(e, req, input, result);
        e.setOwnerUserId(ownerUserId);
        e.setCreatedDate(LocalDateTime.now());
        return e;
    }

    private void applyResult(Simulation e, SimulationRequest req,
                             FinancialEngine.Input input, FinancialCalculationResult result) {
        e.setClientId(req.getClientId());
        e.setVehicleId(req.getVehicleId());
        e.setFinancialEntityId(req.getFinancialEntityId());
        e.setCurrency(req.getCurrency() != null ? req.getCurrency() : "PEN");
        e.setVehiclePrice(input.vehiclePrice());
        e.setDownPaymentPercent(input.downPaymentPercent());
        e.setDownPayment(input.vehiclePrice().multiply(input.downPaymentPercent())
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        e.setFinancedAmount(result.getFinancedAmount());
        e.setTermMonths(input.termMonths());
        e.setFirstPaymentDate(input.firstPaymentDate());
        e.setDisbursementDate(input.firstPaymentDate().minusMonths(1));
        e.setPaymentDay(input.paymentDay());
        e.setGraceType(input.graceType().name());
        e.setGraceMonths(input.graceMonths());
        e.setBalloonEnabled(input.balloonPercent().signum() > 0);
        e.setBalloonPercent(input.balloonPercent());
        e.setBalloonAmount(result.getBalloonAmount());
        e.setCreditLifeInsuranceMonthlyPercent(input.creditLifeInsuranceMonthlyPercent());
        e.setVehicleInsuranceAnnualPercent(input.vehicleInsuranceAnnualPercent());
        e.setTeaPercent(result.getTea());
        e.setTemPercent(result.getTem());
        e.setCokTeaPercent(result.getCokTeaPercent());
        e.setCokTemPercent(result.getCokTemPercent());
        e.setMonthlyPayment(result.getMonthlyPayment());
        e.setVan(result.getVan());
        e.setTirPercent(result.getTir());
        e.setTceaPercent(result.getTcea());
        e.setTotalInterest(result.getTotalInterest());
        e.setTotalInsurance(result.getTotalInsurance());
        e.setTotalFees(result.getTotalCommissions());
        e.setTotalPayment(result.getTotalPayment());
        e.setStatus("Guardado");
        e.setArchived(false);
    }

    private PaymentSchedule toPeriod(Long simulationId, PaymentRow r) {
        PaymentSchedule s = new PaymentSchedule();
        s.setSimulationId(simulationId); s.setPeriod(r.getPeriod()); s.setDate(r.getDate());
        s.setInitialBalance(r.getInitialBalance()); s.setPayment(r.getPayment());
        s.setBalloonPayment(r.getBalloonPayment()); s.setInterest(r.getInterest());
        s.setAmortization(r.getAmortization()); s.setInsurance(r.getInsurance());
        s.setCreditLifeInsurance(r.getCreditLifeInsurance()); s.setVehicleInsurance(r.getVehicleInsurance());
        s.setCommission(r.getCommission()); s.setTotalPayment(r.getTotalPayment());
        s.setFinalFlow(r.getFinalFlow()); s.setBaseFlow(r.getBaseFlow());
        s.setFinalBalance(r.getFinalBalance()); s.setGraceType(r.getGraceType());
        return s;
    }

    private PaymentRow fromPeriod(PaymentSchedule s) {
        PaymentRow r = new PaymentRow();
        r.setPeriod(s.getPeriod()); r.setDate(s.getDate());
        r.setInitialBalance(s.getInitialBalance()); r.setPayment(s.getPayment());
        r.setBalloonPayment(s.getBalloonPayment()); r.setInterest(s.getInterest());
        r.setAmortization(s.getAmortization()); r.setInsurance(s.getInsurance());
        r.setCreditLifeInsurance(s.getCreditLifeInsurance()); r.setVehicleInsurance(s.getVehicleInsurance());
        r.setCommission(s.getCommission()); r.setTotalPayment(s.getTotalPayment());
        r.setFinalFlow(s.getFinalFlow()); r.setBaseFlow(s.getBaseFlow());
        r.setFinalBalance(s.getFinalBalance()); r.setGraceType(s.getGraceType());
        return r;
    }

    private SimulationResponse toResponse(Simulation e, List<PaymentRow> schedule) {
        SimulationResponse r = new SimulationResponse();
        r.setId(e.getId()); r.setCode(e.getCode()); r.setClientId(e.getClientId());
        r.setVehicleId(e.getVehicleId()); r.setFinancialEntityId(e.getFinancialEntityId());
        r.setCurrency(e.getCurrency()); r.setVehiclePrice(e.getVehiclePrice());
        r.setDownPaymentPercent(e.getDownPaymentPercent()); r.setFinancedAmount(e.getFinancedAmount());
        r.setTermMonths(e.getTermMonths()); r.setFirstPaymentDate(e.getFirstPaymentDate());
        r.setPaymentDay(e.getPaymentDay()); r.setGraceType(e.getGraceType());
        r.setGraceMonths(e.getGraceMonths()); r.setBalloonPercent(e.getBalloonPercent());
        r.setCreditLifeInsuranceMonthlyPercent(e.getCreditLifeInsuranceMonthlyPercent());
        r.setVehicleInsuranceAnnualPercent(e.getVehicleInsuranceAnnualPercent());
        r.setMonthlyPayment(e.getMonthlyPayment()); r.setTeaPercent(e.getTeaPercent());
        r.setTemPercent(e.getTemPercent()); r.setCokTeaPercent(e.getCokTeaPercent());
        r.setCokTemPercent(e.getCokTemPercent()); r.setTirPercent(e.getTirPercent());
        r.setTceaPercent(e.getTceaPercent()); r.setVan(e.getVan());
        r.setTotalInterest(e.getTotalInterest()); r.setTotalInsurance(e.getTotalInsurance());
        r.setTotalFees(e.getTotalFees()); r.setTotalPayment(e.getTotalPayment());
        r.setStatus(e.getStatus()); r.setCreatedDate(e.getCreatedDate());
        r.setSchedule(schedule);
        return r;
    }
}
