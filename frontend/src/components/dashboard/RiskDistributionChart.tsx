import Chart from "react-apexcharts";
import { ApexOptions } from "apexcharts";

interface RiskDistributionProps {
  low?: number;
  medium?: number;
  high?: number;
  critical?: number;
}

export default function RiskDistributionChart({
  low = 0,
  medium = 0,
  high = 0,
  critical = 0,
}: RiskDistributionProps) {
  const hasData = low + medium + high + critical > 0;

  const options: ApexOptions = {
    chart: {
      fontFamily: "Outfit, sans-serif",
      type: "donut",
      toolbar: {
        show: false,
      },
    },
    colors: ["#22C55E", "#F59E0B", "#EF4444", "#DC2626"],
    labels: ["Low Risk", "Medium Risk", "High Risk", "Critical Risk"],
    legend: {
      show: true,
      position: "bottom",
    },
    plotOptions: {
      pie: {
        donut: {
          size: "65%",
          labels: {
            show: true,
            total: {
              show: true,
              showAlways: true,
              label: "Total",
              fontSize: "16px",
              fontWeight: 400,
            },
            value: {
              show: true,
              fontSize: "24px",
              fontWeight: 700,
            },
          },
        },
      },
    },
    dataLabels: {
      enabled: false,
    },
    responsive: [
      {
        breakpoint: 640,
        options: {
          chart: {
            width: 300,
          },
          legend: {
            position: "bottom",
          },
        },
      },
    ],
    tooltip: {
      y: {
        formatter: function (value) {
          return `${value} assessments`;
        },
      },
    },
  };

  const series = hasData ? [low, medium, high, critical] : [1, 0, 0, 0];

  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
      <div className="mb-4">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
          Credit Risk Distribution
        </h3>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Assessment breakdown by risk category
        </p>
      </div>

      {!hasData ? (
        <div className="flex h-[300px] items-center justify-center">
          <p className="text-gray-500 dark:text-gray-400">No risk assessment data available</p>
        </div>
      ) : (
        <div id="risk-distribution-chart" className="flex justify-center">
          <Chart options={options} series={series} type="donut" width="100%" height={300} />
        </div>
      )}

      {hasData && (
        <div className="mt-6 grid grid-cols-2 gap-4">
          <div className="flex items-center gap-3">
            <div className="h-3 w-3 rounded-full bg-green-500"></div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">Low Risk</p>
              <p className="text-lg font-semibold text-gray-900 dark:text-white">{low}</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="h-3 w-3 rounded-full bg-yellow-500"></div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">Medium Risk</p>
              <p className="text-lg font-semibold text-gray-900 dark:text-white">{medium}</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="h-3 w-3 rounded-full bg-red-500"></div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">High Risk</p>
              <p className="text-lg font-semibold text-gray-900 dark:text-white">{high}</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="h-3 w-3 rounded-full bg-red-700"></div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">Critical Risk</p>
              <p className="text-lg font-semibold text-gray-900 dark:text-white">{critical}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
