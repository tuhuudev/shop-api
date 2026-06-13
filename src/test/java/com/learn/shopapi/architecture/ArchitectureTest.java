package com.learn.shopapi.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * RULE KIEN TRUC (ArchUnit). Day la "test" nhung muc dich la ENFORCE cau truc:
 * neu ai do pha vo tang (vi du controller goi thang repository), build se bao loi.
 *
 * Chay cung mvn test. importOptions = DoNotIncludeTests: chi soi code chinh, bo qua test.
 */
@AnalyzeClasses(packages = "com.learn.shopapi", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // Controller chi duoc goi Service, KHONG goi thang Repository (phai qua tang service).
    @ArchTest
    static final ArchRule controller_khong_goi_thang_repository =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .because("Controller phai di qua Service, khong cham truc tiep Repository");

    // Repository la tang thap nhat -> khong duoc biet ve Controller/Service.
    @ArchTest
    static final ArchRule repository_khong_phu_thuoc_tang_tren =
            noClasses().that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..service..");

    // Entity (bang DB) khong duoc phu thuoc cac tang phia tren.
    @ArchTest
    static final ArchRule entity_khong_phu_thuoc_tang_tren =
            noClasses().that().resideInAPackage("..entity..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..controller..", "..service..", "..repository..", "..dto..")
                    .because("Entity la tang du lieu, khong duoc biet ve API/nghiep vu");

    // Bat buoc Dependency Injection qua constructor (khong field injection) cho tuong minh, de test.
    @ArchTest
    static final ArchRule khong_field_injection =
            noFields().should().beAnnotatedWith(Autowired.class)
                    .because("Du an dung constructor injection (xem cac constructor trong service/controller)");

    // @RestController phai dat ten ket thuc bang "Controller" va nam trong package controller.
    @ArchTest
    static final ArchRule restController_dung_ten_va_vi_tri =
            classes().that().areAnnotatedWith(RestController.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .andShould().resideInAPackage("..controller..");

    // Moi class TOP-LEVEL trong package service phai la @Service (giu package service "sach").
    // areTopLevelClasses(): bo qua class long ben trong (vi du LoginAttemptService.Attempt).
    @ArchTest
    static final ArchRule package_service_chi_chua_service =
            classes().that().resideInAPackage("..service..").and().areTopLevelClasses()
                    .should().beAnnotatedWith(Service.class);
}
